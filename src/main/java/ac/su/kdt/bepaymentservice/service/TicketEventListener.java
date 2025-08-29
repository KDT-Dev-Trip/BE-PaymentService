package ac.su.kdt.bepaymentservice.service;

import ac.su.kdt.bepaymentservice.kafka.ExternalEvent;
import ac.su.kdt.bepaymentservice.metrics.PaymentMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Map;

// 티켓 이벤트 리스너
// 티켓 이벤트 리스너는 티켓 이벤트를 처리하는 데 사용됩니다.
// 이벤트는 사용자 등록, 사용자 로그인, 사용자 로그아웃, 사용자 프로필 업데이트, 사용자 프로필 이미지 변경, 사용자 설정 변경, 팀 생성, 팀 멤버 추가, 팀 동기화 등의 정보를 포함합니다.

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketEventListener {
    
    private final TicketService ticketService;
    private final PaymentEventService paymentEventService;
    private final PaymentMetrics paymentMetrics;
    
    @KafkaListener(topics = {"${kafka.topic.user-events}", "${kafka.topic.auth-events}"})
    public void handleUserEvents(
            @Payload Object rawEvent,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        
        try {
            log.info("Received raw event from topic: {} partition: {} offset: {}", topic, partition, offset);
            log.info("Raw event: {}", rawEvent);
            
            // Map 형태의 이벤트를 ExternalEvent로 변환
            if (rawEvent instanceof Map<?, ?> eventMap) {
                processMapEvent(eventMap);
            } else if (rawEvent instanceof ExternalEvent event) {
                log.info("Processing ExternalEvent: {}", event.getEventType());
                paymentMetrics.incrementKafkaEventReceived(event.getEventType());
                processUserEvent(event);
            } else {
                log.warn("Unknown event type: {}", rawEvent.getClass());
            }
            
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            log.error("Error processing user event from topic: {} - {}", topic, e.getMessage(), e);
            if (rawEvent instanceof ExternalEvent event) {
                paymentMetrics.incrementKafkaEventFailure(event.getEventType());
            }
            acknowledgment.acknowledge();
        }
    }
    
    private void processMapEvent(Map<?, ?> eventMap) {
        try {
            log.info("Processing map event: {}", eventMap);
            
            // event_type 확인
            Object eventTypeObj = eventMap.get("event_type");
            if (eventTypeObj == null) {
                // event_type이 없으면 auth service의 직접 이벤트
                Object authUserIdObj = eventMap.get("authUserId");
                if (authUserIdObj != null) {
                    handleUserSignedUpFromMap(eventMap);
                }
                return;
            }
            
            String eventType = eventTypeObj.toString();
            log.info("Event type: {}", eventType);
            
            switch (eventType) {
                case "user.signed-up":
                case "auth.user-signed-up":
                    handleUserSignedUpFromMap(eventMap);
                    break;
                case "auth.user-logged-out":
                    handleUserLoggedOutFromMap(eventMap);
                    break;
                case "auth.login-failed":
                    handleLoginFailedFromMap(eventMap);
                    break;
                case "auth.account-locked":
                    handleAccountLockedFromMap(eventMap);
                    break;
                case "user.profile-updated":
                    handleUserProfileUpdatedFromMap(eventMap);
                    break;
                case "user.profile-image-changed":
                    handleUserProfileImageChangedFromMap(eventMap);
                    break;
                case "user.settings-changed":
                    handleUserSettingsChangedFromMap(eventMap);
                    break;
                case "auth.team-created":
                    handleTeamCreatedFromMap(eventMap);
                    break;
                case "auth.team-member-added":
                    handleTeamMemberAddedFromMap(eventMap);
                    break;
                case "full.sync":
                    handleFullSyncFromMap(eventMap);
                    break;
                default:
                    log.info("Unhandled map event type: {}", eventType);
            }
            
        } catch (Exception e) {
            log.error("Error processing map event: {}", eventMap, e);
        }
    }
    
    private void handleUserSignedUpFromMap(Map<?, ?> eventMap) {
        try {
            Object userIdObj = eventMap.get("userId");
            Object authUserIdObj = eventMap.get("authUserId");
            
            Long userId = null;
            if (userIdObj != null) {
                userId = Long.parseLong(userIdObj.toString());
            } else if (authUserIdObj != null) {
                // authUserId를 Long으로 변환
                String authUserId = authUserIdObj.toString();
                userId = Math.abs((long) authUserId.hashCode());
            }
            
            if (userId != null) {
                log.info("Processing user sign-up from auth service for user: {}", userId);
                ticketService.getUserTickets(userId);
                log.info("Created ticket account for user: {}", userId);
            }
        } catch (Exception e) {
            log.error("Failed to process user sign-up from map: {}", eventMap, e);
        }
    }
    
    private void handleFullSyncFromMap(Map<?, ?> eventMap) {
        try {
            Object usersObject = eventMap.get("users");
            if (usersObject instanceof java.util.List<?> usersList) {
                log.info("FULL_SYNC 이벤트 처리 시작: {} 명의 사용자", usersList.size());
                
                int syncCount = 0;
                for (Object userObj : usersList) {
                    if (userObj instanceof Map<?, ?> userMap) {
                        try {
                            Object userIdObj = userMap.get("id");
                            if (userIdObj != null) {
                                Long userId = Long.parseLong(userIdObj.toString());
                                ticketService.getUserTickets(userId);
                                syncCount++;
                                log.info("Synchronized ticket account for user: {}", userId);
                            }
                        } catch (Exception e) {
                            log.error("개별 사용자 티켓 계정 생성 중 오류: {}", userMap, e);
                        }
                    }
                }
                log.info("FULL_SYNC 이벤트 처리 완료: {}/{}명의 사용자 티켓 계정 동기화", syncCount, usersList.size());
            }
        } catch (Exception e) {
            log.error("FULL_SYNC 이벤트 처리 중 오류", e);
        }
    }
    
    @KafkaListener(topics = "${kafka.topic.mission-events}")
    public void handleMissionEvents(
            @Payload ExternalEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        
        try {
            log.info("Received mission event: {} from topic: {} partition: {} offset: {}", 
                    event.getEventType(), topic, partition, offset);
            
            paymentMetrics.incrementKafkaEventReceived(event.getEventType());
            processMissionEvent(event);
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            log.error("Error processing mission event: {} - {}", event.getEventId(), e.getMessage(), e);
            paymentMetrics.incrementKafkaEventFailure(event.getEventType());
            acknowledgment.acknowledge();
        }
    }
    
    private void processUserEvent(ExternalEvent event) {
        ExternalEvent.EventType eventType = ExternalEvent.EventType.fromValue(event.getEventType());
        
        switch (eventType) {
            case USER_REGISTERED:
                handleUserRegistered(event);
                break;
            case USER_SIGNED_UP:
                handleUserSignedUp(event);
                break;
            case FULL_SYNC:
                handleFullSync(event);
                break;
            case TEAM_CREATED:
                handleTeamCreated(event);
                break;
            case ACHIEVEMENT_UNLOCKED:
                handleAchievementUnlocked(event);
                break;
            default:
                log.info("Unhandled user event type: {}", event.getEventType());
        }
    }
    
    private void processMissionEvent(ExternalEvent event) {
        ExternalEvent.EventType eventType = ExternalEvent.EventType.fromValue(event.getEventType());
        
        switch (eventType) {
            case MISSION_COMPLETED:
                handleMissionCompleted(event);
                break;
            default:
                log.info("Unhandled mission event type: {}", event.getEventType());
        }
    }
    
    private void handleUserRegistered(ExternalEvent event) {
        Long userId = event.getUserId();
        log.info("Processing user registration for user: {}", userId);
        
        // 신규 사용자 등록시 환영 티켓 지급
        int welcomeTickets = 1;
        ticketService.adjustTickets(userId, welcomeTickets, "Welcome bonus for new user");
        
        log.info("Granted {} welcome tickets to new user: {}", welcomeTickets, userId);
    }
    
    private void handleUserSignedUp(ExternalEvent event) {
        Long userId = event.getUserId();
        log.info("Processing user sign-up from auth service for user: {}", userId);
        
        // Create user ticket account for new user from auth service
        // This will trigger initial ticket creation based on subscription plan
        try {
            ticketService.getUserTickets(userId);
            log.info("Created ticket account for user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to create ticket account for user: {}", userId, e);
        }
    }
    
    private void handleFullSync(ExternalEvent event) {
        log.info("Processing FULL_SYNC event from auth service");
        
        try {
            Object usersObject = event.getData().get("users");
            if (usersObject instanceof java.util.List<?> usersList) {
                log.info("FULL_SYNC 이벤트 처리 시작: {} 명의 사용자", usersList.size());
                
                int syncCount = 0;
                for (Object userObj : usersList) {
                    if (userObj instanceof Map<?, ?> userMap) {
                        try {
                            Object userIdObj = userMap.get("id");
                            if (userIdObj != null) {
                                Long userId = Long.parseLong(userIdObj.toString());
                                ticketService.getUserTickets(userId);
                                syncCount++;
                                log.info("Synchronized ticket account for user: {}", userId);
                            }
                        } catch (Exception e) {
                            log.error("개별 사용자 티켓 계정 생성 중 오류: {}", userMap, e);
                        }
                    }
                }
                log.info("FULL_SYNC 이벤트 처리 완료: {}/{}명의 사용자 티켓 계정 동기화", syncCount, usersList.size());
            }
        } catch (Exception e) {
            log.error("FULL_SYNC 이벤트 처리 중 오류", e);
        }
    }
    
    private void handleTeamCreated(ExternalEvent event) {
        Long userId = event.getUserId();
        Long teamId = event.getTeamId();
        log.info("Processing team creation for user: {} team: {}", userId, teamId);
        
        // 팀 생성시 보너스 티켓 지급
        int teamCreationBonus = 2;
        ticketService.adjustTickets(userId, teamCreationBonus, "Team creation bonus");
        
        log.info("Granted {} team creation bonus tickets to user: {}", teamCreationBonus, userId);
    }
    
    private void handleMissionCompleted(ExternalEvent event) {
        Long userId = event.getUserId();
        Object difficultyObj = event.getData().get("difficulty");
        String difficulty = difficultyObj != null ? difficultyObj.toString() : "EASY";
        
        log.info("Processing mission completion for user: {} with difficulty: {}", userId, difficulty);
        
        // 미션 완료시 난이도에 따른 보너스 티켓 지급
        int bonusTickets = calculateMissionBonus(difficulty);
        if (bonusTickets > 0) {
            ticketService.adjustTickets(userId, bonusTickets, 
                "Mission completion bonus (" + difficulty + ")");
            
            log.info("Granted {} mission completion bonus tickets to user: {}", bonusTickets, userId);
        }
    }
    
    private void handleAchievementUnlocked(ExternalEvent event) {
        Long userId = event.getUserId();
        Object achievementTypeObj = event.getData().get("achievement_type");
        String achievementType = achievementTypeObj != null ? achievementTypeObj.toString() : "UNKNOWN";
        
        log.info("Processing achievement unlock for user: {} achievement: {}", userId, achievementType);
        
        // 업적 해금시 보너스 티켓 지급
        int achievementBonus = calculateAchievementBonus(achievementType);
        if (achievementBonus > 0) {
            ticketService.adjustTickets(userId, achievementBonus, 
                "Achievement unlock bonus (" + achievementType + ")");
            
            log.info("Granted {} achievement bonus tickets to user: {}", achievementBonus, userId);
        }
    }
    
    private int calculateMissionBonus(String difficulty) {
        switch (difficulty.toUpperCase()) {
            case "EASY":
                return 1;
            case "MEDIUM":
                return 2;
            case "HARD":
                return 3;
            case "EXPERT":
                return 5;
            default:
                return 1;
        }
    }
    
    private int calculateAchievementBonus(String achievementType) {
        switch (achievementType.toUpperCase()) {
            case "FIRST_MISSION":
                return 2;
            case "WEEK_STREAK":
                return 3;
            case "MONTH_STREAK":
                return 10;
            case "EXPERT_LEVEL":
                return 15;
            case "TEAM_LEADER":
                return 5;
            default:
                return 1;
        }
    }
    
    private void handleUserLoggedOutFromMap(Map<?, ?> eventMap) {
        try {
            Object authUserIdObj = eventMap.get("auth_user_id");
            Object emailObj = eventMap.get("email");
            Object logoutReasonObj = eventMap.get("logoutReason");
            
            log.info("Processing user logout event: authUserId={}, email={}, reason={}", 
                    authUserIdObj, emailObj, logoutReasonObj);
            
            // 로그아웃 이벤트는 보통 특별한 티켓 처리가 필요하지 않지만, 
            // 필요시 세션 정리나 통계 기록 등을 수행할 수 있음
            
        } catch (Exception e) {
            log.error("Error processing user logout event: {}", eventMap, e);
        }
    }
    
    private void handleLoginFailedFromMap(Map<?, ?> eventMap) {
        try {
            Object emailObj = eventMap.get("email");
            Object failureReasonObj = eventMap.get("failureReason");
            Object attemptCountObj = eventMap.get("attemptCount");
            Object ipAddressObj = eventMap.get("ipAddress");
            
            log.warn("Processing login failed event: email={}, reason={}, attemptCount={}, ip={}", 
                    emailObj, failureReasonObj, attemptCountObj, ipAddressObj);
            
            // 로그인 실패 이벤트는 보안 모니터링 목적으로 기록
            // 필요시 특정 횟수 이상 실패한 사용자에 대한 추가 제재 로직을 추가할 수 있음
            
        } catch (Exception e) {
            log.error("Error processing login failed event: {}", eventMap, e);
        }
    }
    
    private void handleAccountLockedFromMap(Map<?, ?> eventMap) {
        try {
            Object authUserIdObj = eventMap.get("auth_user_id");
            Object emailObj = eventMap.get("email");
            Object lockReasonObj = eventMap.get("lockReason");
            Object lockDurationMinutesObj = eventMap.get("lockDurationMinutes");
            
            log.warn("Processing account locked event: authUserId={}, email={}, reason={}, duration={}분", 
                    authUserIdObj, emailObj, lockReasonObj, lockDurationMinutesObj);
            
            // 계정 잠금 이벤트 처리
            // 필요시 잠금된 계정의 티켓 사용을 일시적으로 제한하는 로직을 추가할 수 있음
            
        } catch (Exception e) {
            log.error("Error processing account locked event: {}", eventMap, e);
        }
    }
    
    private void handleUserProfileUpdatedFromMap(Map<?, ?> eventMap) {
        try {
            Object userIdObj = eventMap.get("user_id");
            Object emailObj = eventMap.get("email");
            Object changesObj = eventMap.get("changes");
            
            log.info("Processing user profile updated event: userId={}, email={}, changes={}", 
                    userIdObj, emailObj, changesObj);
            
            // 프로필 업데이트 이벤트 처리
            // 필요시 사용자 정보 동기화나 특별한 보상 로직을 추가할 수 있음
            
        } catch (Exception e) {
            log.error("Error processing user profile updated event: {}", eventMap, e);
        }
    }
    
    private void handleUserProfileImageChangedFromMap(Map<?, ?> eventMap) {
        try {
            Object userIdObj = eventMap.get("user_id");
            Object emailObj = eventMap.get("email");
            Object oldImageUrlObj = eventMap.get("oldImageUrl");
            Object newImageUrlObj = eventMap.get("newImageUrl");
            
            log.info("Processing user profile image changed event: userId={}, email={}, oldImage={}, newImage={}", 
                    userIdObj, emailObj, oldImageUrlObj, newImageUrlObj);
            
            // 프로필 이미지 변경 이벤트 처리
            // 필요시 이미지 업데이트 보상이나 통계 수집 로직을 추가할 수 있음
            
        } catch (Exception e) {
            log.error("Error processing user profile image changed event: {}", eventMap, e);
        }
    }
    
    private void handleUserSettingsChangedFromMap(Map<?, ?> eventMap) {
        try {
            Object userIdObj = eventMap.get("user_id");
            Object emailObj = eventMap.get("email");
            Object settingCategoryObj = eventMap.get("settingCategory");
            
            log.info("Processing user settings changed event: userId={}, email={}, category={}", 
                    userIdObj, emailObj, settingCategoryObj);
            
            // 사용자 설정 변경 이벤트 처리
            // 필요시 설정에 따른 서비스 동작 변경 로직을 추가할 수 있음
            
        } catch (Exception e) {
            log.error("Error processing user settings changed event: {}", eventMap, e);
        }
    }
    
    private void handleTeamCreatedFromMap(Map<?, ?> eventMap) {
        try {
            Object teamIdObj = eventMap.get("team_id");
            Object creatorUserIdObj = eventMap.get("creator_user_id");
            Object teamNameObj = eventMap.get("teamName");
            
            log.info("Processing team created event: teamId={}, creatorUserId={}, teamName={}", 
                    teamIdObj, creatorUserIdObj, teamNameObj);
            
            // 팀 생성 이벤트 처리
            if (creatorUserIdObj != null) {
                try {
                    Long creatorUserId = Long.parseLong(creatorUserIdObj.toString());
                    // 팀 생성자에게 보너스 티켓 지급
                    int teamCreationBonus = 3;
                    ticketService.adjustTickets(creatorUserId, teamCreationBonus, "Team creation bonus");
                    log.info("Granted {} team creation bonus tickets to user: {}", teamCreationBonus, creatorUserId);
                } catch (NumberFormatException e) {
                    log.warn("Failed to parse creator user ID: {}", creatorUserIdObj);
                }
            }
            
        } catch (Exception e) {
            log.error("Error processing team created event: {}", eventMap, e);
        }
    }
    
    private void handleTeamMemberAddedFromMap(Map<?, ?> eventMap) {
        try {
            Object teamIdObj = eventMap.get("team_id");
            Object userIdObj = eventMap.get("user_id");
            Object memberRoleObj = eventMap.get("memberRole");
            
            log.info("Processing team member added event: teamId={}, userId={}, role={}", 
                    teamIdObj, userIdObj, memberRoleObj);
            
            // 팀 멤버 추가 이벤트 처리
            if (userIdObj != null) {
                try {
                    Long userId = Long.parseLong(userIdObj.toString());
                    // 팀 참여자에게 환영 티켓 지급
                    int welcomeBonus = 1;
                    ticketService.adjustTickets(userId, welcomeBonus, "Team join welcome bonus");
                    log.info("Granted {} team join bonus tickets to user: {}", welcomeBonus, userId);
                } catch (NumberFormatException e) {
                    log.warn("Failed to parse user ID: {}", userIdObj);
                }
            }
            
        } catch (Exception e) {
            log.error("Error processing team member added event: {}", eventMap, e);
        }
    }
}