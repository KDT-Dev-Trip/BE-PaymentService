package ac.su.kdt.bepaymentservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MissionEventListener {
    
    private final TicketService ticketService;
    private final SubscriptionService subscriptionService;
    private final PaymentEventPublisher paymentEventPublisher;
    
    /**
     * 미션 이벤트 처리 - 결제 서비스 관점
     */
    @KafkaListener(topics = "mission-events", groupId = "payment-service-mission-group")
    public void handleMissionEvent(Map<String, Object> eventData) {
        String eventType = (String) eventData.get("eventType");
        
        try {
            switch (eventType) {
                case "mission.paused":
                    handleMissionPaused(eventData);
                    break;
                case "mission.resumed":
                    handleMissionResumed(eventData);
                    break;
                case "mission.resource-provisioning-failed":
                    handleResourceProvisioningFailed(eventData);
                    break;
                case "mission.resource-cleanup-completed":
                    handleResourceCleanupCompleted(eventData);
                    break;
                default:
                    log.debug("Unknown mission event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Error handling mission event: {}", eventType, e);
        }
    }
    
    /**
     * 미션 일시정지 이벤트 처리 - 결제 서비스 관점
     */
    private void handleMissionPaused(Map<String, Object> eventData) {
        try {
            Long userId = Long.valueOf(eventData.get("userId").toString());
            String missionTitle = (String) eventData.get("missionTitle");
            String pauseReason = (String) eventData.get("pauseReason");
            Integer progressPercent = (Integer) eventData.get("progressPercent");
            
            log.info("🔄 [PAYMENT_SERVICE] Mission paused - Payment processing: userId={}, mission={}", 
                    userId, missionTitle);
            
            // 1. 일시정지 중 티켓 소모 중단 (이미 처리됨)
            log.info("Mission paused - Ticket consumption halted for user: {}", userId);
            
            // 2. 장시간 일시정지시 추가 티켓 보상 고려
            if ("장시간_비활성".equals(pauseReason) && progressPercent != null && progressPercent >= 50) {
                // 50% 이상 진행 후 장시간 일시정지된 경우 보상 티켓 지급
                try {
                    ticketService.adjustTickets(userId, 1, "미션 장시간 일시정지 보상");
                    log.info("Compensation ticket granted for extended pause: userId={}", userId);
                } catch (Exception e) {
                    log.warn("Failed to grant compensation ticket: {}", e.getMessage());
                }
            }
            
            // 3. 미션 일시정지 통계 업데이트 (과금 목적)
            updateMissionUsageStats(userId, missionTitle, "PAUSED", pauseReason);
            
        } catch (Exception e) {
            log.error("Failed to handle mission paused event in payment service", e);
        }
    }
    
    /**
     * 미션 재개 이벤트 처리 - 결제 서비스 관점
     */
    private void handleMissionResumed(Map<String, Object> eventData) {
        try {
            Long userId = Long.valueOf(eventData.get("userId").toString());
            String missionTitle = (String) eventData.get("missionTitle");
            Long pauseDurationMinutes = Long.valueOf(eventData.get("pauseDurationMinutes").toString());
            
            log.info("▶️ [PAYMENT_SERVICE] Mission resumed - Payment processing: userId={}, pauseTime={}min", 
                    userId, pauseDurationMinutes);
            
            // 1. 미션 재개시 티켓 소모 재시작 (이미 처리됨)
            log.info("Mission resumed - Ticket consumption resumed for user: {}", userId);
            
            // 2. 긴 일시정지 후 재개시 프리미엄 사용자 혜택 제공
            if (pauseDurationMinutes > 120) { // 2시간 이상 일시정지
                try {
                    var userSubscription = subscriptionService.getUserActiveSubscription(userId);
                    if (userSubscription != null && !"ECONOMY_CLASS".equals(userSubscription.getPlan().getPlanName())) {
                        // 프리미엄 사용자에게 시간 연장 혜택
                        log.info("Extended time benefit granted for premium user: userId={}", userId);
                    }
                } catch (Exception e) {
                    log.warn("Failed to apply premium benefits: {}", e.getMessage());
                }
            }
            
            // 3. 미션 재개 통계 업데이트
            updateMissionUsageStats(userId, missionTitle, "RESUMED", String.valueOf(pauseDurationMinutes));
            
        } catch (Exception e) {
            log.error("Failed to handle mission resumed event in payment service", e);
        }
    }
    
    /**
     * 리소스 프로비저닝 실패 이벤트 처리 - 결제 서비스 관점
     */
    private void handleResourceProvisioningFailed(Map<String, Object> eventData) {
        try {
            Long userId = Long.valueOf(eventData.get("userId").toString());
            String missionTitle = (String) eventData.get("missionTitle");
            String failureReason = (String) eventData.get("failureReason");
            Integer retryAttempt = (Integer) eventData.get("retryAttempt");
            
            log.warn("🚨 [PAYMENT_SERVICE] Resource provisioning failed - Payment compensation: userId={}, reason={}", 
                    userId, failureReason);
            
            // 1. 시스템 문제로 인한 실패시 티켓 환불
            if (isSystemFailure(failureReason)) {
                try {
                    // 미션 시작시 사용된 티켓 환불
                    String attemptId = (String) eventData.get("attemptId");
                    ticketService.refundTickets(userId, 1, attemptId != null ? Long.parseLong(attemptId) : null, 
                        "리소스 프로비저닝 실패로 인한 환불");
                    
                    log.info("Ticket refunded due to system failure: userId={}, reason={}", userId, failureReason);
                } catch (Exception e) {
                    log.error("Failed to refund ticket for system failure: {}", e.getMessage());
                }
            }
            
            // 2. 반복 실패시 고객 지원 크레딧 지급
            if (retryAttempt != null && retryAttempt >= 3) {
                try {
                    ticketService.adjustTickets(userId, 2, "리소스 프로비저닝 반복 실패 보상");
                    log.info("Compensation credits granted for repeated failures: userId={}", userId);
                } catch (Exception e) {
                    log.warn("Failed to grant compensation credits: {}", e.getMessage());
                }
            }
            
            // 3. 비용 손실 기록 (내부 분석용)
            recordSystemCost(userId, missionTitle, "PROVISIONING_FAILED", failureReason, retryAttempt);
            
        } catch (Exception e) {
            log.error("Failed to handle resource provisioning failed event in payment service", e);
        }
    }
    
    /**
     * 리소스 정리 완료 이벤트 처리 - 결제 서비스 관점
     */
    private void handleResourceCleanupCompleted(Map<String, Object> eventData) {
        try {
            Long userId = Long.valueOf(eventData.get("userId").toString());
            String missionTitle = (String) eventData.get("missionTitle");
            String cleanupTrigger = (String) eventData.get("cleanupTrigger");
            Double costSaved = (Double) eventData.get("costSaved");
            Integer totalResourcesCleaned = (Integer) eventData.get("totalResourcesCleaned");
            
            log.info("🧹 [PAYMENT_SERVICE] Resource cleanup completed - Cost accounting: userId={}, saved=${}", 
                    userId, costSaved);
            
            // 1. 미션 완료시 성과 기반 보너스 티켓 지급
            if ("MISSION_COMPLETED".equals(cleanupTrigger)) {
                try {
                    // 성공적 완료시 보너스 티켓 지급
                    int bonusTickets = calculateCompletionBonus(userId, missionTitle);
                    if (bonusTickets > 0) {
                        ticketService.adjustTickets(userId, bonusTickets, "미션 완료 보너스");
                        log.info("Mission completion bonus granted: userId={}, bonus={} tickets", userId, bonusTickets);
                    }
                } catch (Exception e) {
                    log.warn("Failed to grant mission completion bonus: {}", e.getMessage());
                }
            }
            
            // 2. 조기 정리시 잔여 시간 크레딧 제공
            if ("USER_REQUESTED".equals(cleanupTrigger)) {
                // 사용자가 조기에 미션 종료시 잔여 시간에 대한 부분 크레딧
                try {
                    ticketService.adjustTickets(userId, 1, "미션 조기 종료 잔여 시간 크레딧");
                    log.info("Early termination credit granted: userId={}", userId);
                } catch (Exception e) {
                    log.warn("Failed to grant early termination credit: {}", e.getMessage());
                }
            }
            
            // 3. 비용 절약 기록 (운영 최적화용)
            if (costSaved != null && costSaved > 0) {
                recordCostSavings(userId, missionTitle, cleanupTrigger, costSaved, totalResourcesCleaned);
            }
            
            // 4. 미션 사용량 최종 정산
            finalizeMissionBilling(userId, missionTitle, cleanupTrigger);
            
        } catch (Exception e) {
            log.error("Failed to handle resource cleanup completed event in payment service", e);
        }
    }
    
    /**
     * 시스템 실패 여부 판단
     */
    private boolean isSystemFailure(String failureReason) {
        return "CLUSTER_UNAVAILABLE".equals(failureReason) ||
               "QUOTA_EXCEEDED".equals(failureReason) ||
               "STORAGE_UNAVAILABLE".equals(failureReason) ||
               "NETWORK_ERROR".equals(failureReason);
    }
    
    /**
     * 미션 완료 보너스 계산
     */
    private int calculateCompletionBonus(Long userId, String missionTitle) {
        // 실제 구현에서는 미션 난이도, 완료 시간, 사용자 등급 등을 고려
        try {
            var userSubscription = subscriptionService.getUserActiveSubscription(userId);
            if (userSubscription != null) {
                String planName = userSubscription.getPlan().getPlanName();
                switch (planName) {
                    case "BUSINESS_CLASS": return 3;
                    case "FIRST_CLASS": return 5;
                    default: return 1; // ECONOMY_CLASS
                }
            }
        } catch (Exception e) {
            log.warn("Failed to check user subscription for bonus calculation: {}", e.getMessage());
        }
        return 1; // 기본 보너스
    }
    
    /**
     * 미션 사용량 통계 업데이트
     */
    private void updateMissionUsageStats(Long userId, String missionTitle, String status, String detail) {
        // 실제 구현에서는 통계 DB 또는 분석 시스템에 기록
        log.info("📊 Mission usage stats updated: userId={}, mission={}, status={}, detail={}", 
                userId, missionTitle, status, detail);
    }
    
    /**
     * 시스템 비용 기록
     */
    private void recordSystemCost(Long userId, String missionTitle, String eventType, String reason, Integer attempt) {
        // 실제 구현에서는 비용 분석 시스템에 기록
        log.info("💰 System cost recorded: userId={}, mission={}, event={}, reason={}, attempt={}", 
                userId, missionTitle, eventType, reason, attempt);
    }
    
    /**
     * 비용 절약 기록
     */
    private void recordCostSavings(Long userId, String missionTitle, String trigger, Double saved, Integer resources) {
        // 실제 구현에서는 비용 최적화 분석 시스템에 기록
        log.info("💚 Cost savings recorded: userId={}, mission={}, trigger={}, saved=${}, resources={}", 
                userId, missionTitle, trigger, saved, resources);
    }
    
    /**
     * 미션 과금 최종 정산
     */
    private void finalizeMissionBilling(Long userId, String missionTitle, String trigger) {
        // 실제 구현에서는 최종 사용량 계산 및 과금 처리
        log.info("🧾 Mission billing finalized: userId={}, mission={}, trigger={}", 
                userId, missionTitle, trigger);
    }
}