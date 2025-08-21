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

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketEventListener {
    
    private final TicketService ticketService;
    private final PaymentEventService paymentEventService;
    private final PaymentMetrics paymentMetrics;
    
    @KafkaListener(topics = "${kafka.topic.user-events}")
    public void handleUserEvents(
            @Payload ExternalEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        
        try {
            log.info("Received user event: {} from topic: {} partition: {} offset: {}", 
                    event.getEventType(), topic, partition, offset);
            
            paymentMetrics.incrementKafkaEventReceived(event.getEventType());
            processUserEvent(event);
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            log.error("Error processing user event: {} - {}", event.getEventId(), e.getMessage(), e);
            paymentMetrics.incrementKafkaEventFailure(event.getEventType());
            // 에러 발생시에도 acknowledge하여 메시지가 재처리되지 않도록 함
            // 실제 운영에서는 DLQ(Dead Letter Queue)로 보내거나 재처리 로직 구현 필요
            acknowledgment.acknowledge();
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
}