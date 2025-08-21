package ac.su.kdt.bepaymentservice.service;

import ac.su.kdt.bepaymentservice.kafka.ExternalEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TicketEventListener 단위 테스트")
class TicketEventListenerTest {
    
    @Mock
    private TicketService ticketService;
    
    @Mock
    private PaymentEventService paymentEventService;
    
    @Mock
    private Acknowledgment acknowledgment;
    
    @InjectMocks
    private TicketEventListener ticketEventListener;
    
    @Test
    @DisplayName("사용자 등록 이벤트 처리 시 환영 티켓을 지급한다")
    void handleUserRegistered_GrantsWelcomeTickets() {
        // Given
        ExternalEvent event = ExternalEvent.builder()
                .eventId("evt-001")
                .eventType("user.registered")
                .userId(1L)
                .timestamp(LocalDateTime.now())
                .data(new HashMap<>())
                .build();
        
        // When
        ticketEventListener.handleUserEvents(event, "user-events", 0, 1L, acknowledgment);
        
        // Then
        verify(ticketService).adjustTickets(1L, 1, "Welcome bonus for new user");
        verify(acknowledgment).acknowledge();
    }
    
    @Test
    @DisplayName("팀 생성 이벤트 처리 시 보너스 티켓을 지급한다")
    void handleTeamCreated_GrantsBonusTickets() {
        // Given
        ExternalEvent event = ExternalEvent.builder()
                .eventId("evt-002")
                .eventType("team.created")
                .userId(1L)
                .teamId(100L)
                .timestamp(LocalDateTime.now())
                .data(new HashMap<>())
                .build();
        
        // When
        ticketEventListener.handleUserEvents(event, "user-events", 0, 1L, acknowledgment);
        
        // Then
        verify(ticketService).adjustTickets(1L, 2, "Team creation bonus");
        verify(acknowledgment).acknowledge();
    }
    
    @Test
    @DisplayName("업적 해금 이벤트 처리 시 업적별 보너스 티켓을 지급한다")
    void handleAchievementUnlocked_GrantsAchievementBonus() {
        // Given
        Map<String, Object> data = new HashMap<>();
        data.put("achievement_type", "FIRST_MISSION");
        
        ExternalEvent event = ExternalEvent.builder()
                .eventId("evt-003")
                .eventType("achievement.unlocked")
                .userId(1L)
                .timestamp(LocalDateTime.now())
                .data(data)
                .build();
        
        // When
        ticketEventListener.handleUserEvents(event, "user-events", 0, 1L, acknowledgment);
        
        // Then
        verify(ticketService).adjustTickets(1L, 2, "Achievement unlock bonus (FIRST_MISSION)");
        verify(acknowledgment).acknowledge();
    }
    
    @Test
    @DisplayName("미션 완료 이벤트 처리 시 난이도별 보너스 티켓을 지급한다")
    void handleMissionCompleted_GrantsDifficultyBasedBonus() {
        // Given
        Map<String, Object> data = new HashMap<>();
        data.put("difficulty", "HARD");
        data.put("mission_id", "mission-123");
        
        ExternalEvent event = ExternalEvent.builder()
                .eventId("evt-004")
                .eventType("mission.completed")
                .userId(1L)
                .timestamp(LocalDateTime.now())
                .data(data)
                .build();
        
        // When
        ticketEventListener.handleMissionEvents(event, "mission-events", 0, 1L, acknowledgment);
        
        // Then
        verify(ticketService).adjustTickets(1L, 3, "Mission completion bonus (HARD)");
        verify(acknowledgment).acknowledge();
    }
    
    @Test
    @DisplayName("이벤트 처리 중 예외 발생시 acknowledge 처리한다")
    void handleEvent_ExceptionOccurred_StillAcknowledges() {
        // Given
        ExternalEvent event = ExternalEvent.builder()
                .eventId("evt-005")
                .eventType("user.registered")
                .userId(1L)
                .timestamp(LocalDateTime.now())
                .data(new HashMap<>())
                .build();
        
        doThrow(new RuntimeException("Ticket service error"))
                .when(ticketService).adjustTickets(anyLong(), anyInt(), anyString());
        
        // When
        ticketEventListener.handleUserEvents(event, "user-events", 0, 1L, acknowledgment);
        
        // Then
        verify(acknowledgment).acknowledge();
    }
    
    @Test
    @DisplayName("알려지지 않은 이벤트 타입은 무시한다")
    void handleUnknownEventType_IgnoresEvent() {
        // Given
        ExternalEvent event = ExternalEvent.builder()
                .eventId("evt-006")
                .eventType("unknown.event")
                .userId(1L)
                .timestamp(LocalDateTime.now())
                .data(new HashMap<>())
                .build();
        
        // When
        ticketEventListener.handleUserEvents(event, "user-events", 0, 1L, acknowledgment);
        
        // Then
        verify(ticketService, never()).adjustTickets(anyLong(), anyInt(), anyString());
        verify(acknowledgment).acknowledge();
    }
}