package ac.su.kdt.bepaymentservice.service;

import ac.su.kdt.bepaymentservice.dto.TicketDto;
import ac.su.kdt.bepaymentservice.entity.Subscription;
import ac.su.kdt.bepaymentservice.entity.SubscriptionPlan;
import ac.su.kdt.bepaymentservice.entity.TicketTransaction;
import ac.su.kdt.bepaymentservice.entity.UserTicket;
import ac.su.kdt.bepaymentservice.repository.SubscriptionRepository;
import ac.su.kdt.bepaymentservice.repository.TicketTransactionRepository;
import ac.su.kdt.bepaymentservice.repository.UserTicketRepository;
import ac.su.kdt.bepaymentservice.metrics.PaymentMetrics;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("TicketService 단위 테스트")
class TicketServiceTest {

    @Mock
    private UserTicketRepository userTicketRepository;

    @Mock
    private TicketTransactionRepository ticketTransactionRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private PaymentEventService paymentEventService;

    @Mock
    private PaymentMetrics paymentMetrics;

    @Mock
    private PaymentEventPublisher paymentEventPublisher;

    @InjectMocks
    private TicketService ticketService;

    private UserTicket testUserTicket;
    private Subscription testSubscription;
    private SubscriptionPlan testPlan;
    private Timer.Sample mockTimer;

    @BeforeEach
    void setUp() {
        mockTimer = Timer.start();
        
        testUserTicket = UserTicket.builder()
            .id(1L)
            .userId(100L)
            .currentTickets(10)
            .lastTicketRefill(LocalDateTime.now().minusHours(2))
            .nextRefillAt(LocalDateTime.now().plusHours(22))
            .build();

        testPlan = SubscriptionPlan.builder()
            .id(1L)
            .planName("BASIC")
            .ticketLimit(50)
            .ticketRefillAmount(10)
            .ticketRefillIntervalHours(24)
            .build();

        testSubscription = Subscription.builder()
            .id(1L)
            .userId(100L)
            .plan(testPlan)
            .status(Subscription.SubscriptionStatus.ACTIVE)
            .build();
    }

    @Test
    @DisplayName("사용자 티켓 조회 - 기존 티켓 존재")
    void getUserTickets_ExistingUser() {
        // Given
        given(userTicketRepository.findByUserId(100L)).willReturn(Optional.of(testUserTicket));

        // When
        TicketDto result = ticketService.getUserTickets(100L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCurrentTickets()).isEqualTo(10);
        assertThat(result.getUserId()).isEqualTo(100L);
        verify(userTicketRepository).findByUserId(100L);
    }

    @Test
    @DisplayName("사용자 티켓 조회 - 신규 사용자 티켓 생성")
    void getUserTickets_NewUser() {
        // Given
        given(userTicketRepository.findByUserId(200L)).willReturn(Optional.empty());
        given(subscriptionRepository.findByUserIdAndStatusInOrderByCreatedAtDesc(eq(200L), anyList()))
            .willReturn(List.of(testSubscription));
        given(userTicketRepository.save(any(UserTicket.class))).willReturn(testUserTicket);
        given(ticketTransactionRepository.save(any(TicketTransaction.class)))
            .willReturn(TicketTransaction.builder().build());

        // When
        TicketDto result = ticketService.getUserTickets(200L);

        // Then
        assertThat(result).isNotNull();
        verify(userTicketRepository).save(any(UserTicket.class));
        verify(ticketTransactionRepository).save(any(TicketTransaction.class));
    }

    @Test
    @DisplayName("티켓 사용 - 성공")
    void useTickets_Success() {
        // Given
        given(paymentMetrics.startTicketTimer()).willReturn(mockTimer);
        given(userTicketRepository.findByUserId(100L)).willReturn(Optional.of(testUserTicket));
        given(userTicketRepository.save(any(UserTicket.class))).willReturn(testUserTicket);
        given(ticketTransactionRepository.save(any(TicketTransaction.class)))
            .willReturn(TicketTransaction.builder().build());
        given(subscriptionRepository.findByUserIdAndStatusInOrderByCreatedAtDesc(eq(100L), anyList()))
            .willReturn(List.of(testSubscription));

        // When
        boolean result = ticketService.useTickets(100L, 5, 123L, "Mission attempt");

        // Then
        assertThat(result).isTrue();
        verify(userTicketRepository).save(any(UserTicket.class));
        verify(ticketTransactionRepository).save(any(TicketTransaction.class));
        verify(paymentEventService).publishTicketsUsed(eq(100L), eq(5), anyInt());
        verify(paymentMetrics).incrementTicketUsed(5);
        verify(paymentMetrics).recordTicketProcessingTime(mockTimer);
    }

    @Test
    @DisplayName("티켓 사용 - 잔액 부족")
    void useTickets_InsufficientBalance() {
        // Given
        given(paymentMetrics.startTicketTimer()).willReturn(mockTimer);
        given(userTicketRepository.findByUserId(100L)).willReturn(Optional.of(testUserTicket));

        // When - try to use more tickets than available
        boolean result = ticketService.useTickets(100L, 15, 123L, "Mission attempt");

        // Then
        assertThat(result).isFalse();
        verify(userTicketRepository, never()).save(any(UserTicket.class));
        verify(ticketTransactionRepository, never()).save(any(TicketTransaction.class));
        verify(paymentEventService, never()).publishTicketsUsed(anyLong(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("티켓 환불 - 성공")
    void refundTickets_Success() {
        // Given
        given(userTicketRepository.findByUserId(100L)).willReturn(Optional.of(testUserTicket));
        given(userTicketRepository.save(any(UserTicket.class))).willReturn(testUserTicket);
        given(ticketTransactionRepository.save(any(TicketTransaction.class)))
            .willReturn(TicketTransaction.builder().build());

        // When
        ticketService.refundTickets(100L, 3, 123L, "Mission failed");

        // Then
        verify(userTicketRepository).save(any(UserTicket.class));
        verify(ticketTransactionRepository).save(any(TicketTransaction.class));
        verify(paymentEventService).publishTicketsRefunded(eq(100L), eq(3), anyInt());
        verify(paymentMetrics).incrementTicketRefunded(3);
    }

    @Test
    @DisplayName("티켓 자동 충전 처리 - 성공")
    void processTicketRefills_Success() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        List<UserTicket> eligibleUsers = List.of(testUserTicket);
        
        given(userTicketRepository.findUsersEligibleForRefill(any(LocalDateTime.class)))
            .willReturn(eligibleUsers);
        given(subscriptionRepository.findByUserIdAndStatusInOrderByCreatedAtDesc(eq(100L), anyList()))
            .willReturn(List.of(testSubscription));
        given(userTicketRepository.save(any(UserTicket.class))).willReturn(testUserTicket);
        given(ticketTransactionRepository.save(any(TicketTransaction.class)))
            .willReturn(TicketTransaction.builder().build());

        // When
        ticketService.processTicketRefills();

        // Then
        verify(userTicketRepository).findUsersEligibleForRefill(any(LocalDateTime.class));
        verify(userTicketRepository).save(any(UserTicket.class));
        verify(ticketTransactionRepository).save(any(TicketTransaction.class));
        verify(paymentEventService).publishTicketsRefilled(eq(100L), anyInt(), anyInt());
    }

    @Test
    @DisplayName("티켓 자동 충전 - 활성 구독 없음")
    void processTicketRefills_NoActiveSubscription() {
        // Given
        List<UserTicket> eligibleUsers = List.of(testUserTicket);
        
        given(userTicketRepository.findUsersEligibleForRefill(any(LocalDateTime.class)))
            .willReturn(eligibleUsers);
        given(subscriptionRepository.findByUserIdAndStatusInOrderByCreatedAtDesc(eq(100L), anyList()))
            .willReturn(Collections.emptyList());

        // When
        ticketService.processTicketRefills();

        // Then
        verify(userTicketRepository).findUsersEligibleForRefill(any(LocalDateTime.class));
        // No save should occur when no active subscription
    }

    @Test
    @DisplayName("티켓 자동 충전 - 이미 한도에 도달")
    void processTicketRefills_AlreadyAtLimit() {
        // Given
        UserTicket maxTicketUser = UserTicket.builder()
            .id(1L)
            .userId(100L)
            .currentTickets(50) // Already at limit
            .lastTicketRefill(LocalDateTime.now().minusHours(2))
            .nextRefillAt(LocalDateTime.now().plusHours(22))
            .build();
            
        List<UserTicket> eligibleUsers = List.of(maxTicketUser);
        
        given(userTicketRepository.findUsersEligibleForRefill(any(LocalDateTime.class)))
            .willReturn(eligibleUsers);
        given(subscriptionRepository.findByUserIdAndStatusInOrderByCreatedAtDesc(eq(100L), anyList()))
            .willReturn(List.of(testSubscription));
        given(userTicketRepository.save(any(UserTicket.class))).willReturn(maxTicketUser);

        // When
        ticketService.processTicketRefills();

        // Then
        verify(userTicketRepository).save(any(UserTicket.class)); // Only to update next refill time
        // No transaction should be recorded since no tickets added
    }

    @Test
    @DisplayName("관리자 티켓 조정 - 양수 조정")
    void adjustTickets_PositiveAdjustment() {
        // Given
        given(userTicketRepository.findByUserId(100L)).willReturn(Optional.of(testUserTicket));
        given(userTicketRepository.save(any(UserTicket.class))).willReturn(testUserTicket);
        given(ticketTransactionRepository.save(any(TicketTransaction.class)))
            .willReturn(TicketTransaction.builder().build());

        // When
        ticketService.adjustTickets(100L, 5, "Admin grant");

        // Then
        verify(userTicketRepository).save(any(UserTicket.class));
        verify(ticketTransactionRepository).save(any(TicketTransaction.class));
        verify(paymentMetrics).incrementTicketGranted(5);
    }

    @Test
    @DisplayName("관리자 티켓 조정 - 음수 조정")
    void adjustTickets_NegativeAdjustment() {
        // Given
        given(userTicketRepository.findByUserId(100L)).willReturn(Optional.of(testUserTicket));
        given(userTicketRepository.save(any(UserTicket.class))).willReturn(testUserTicket);
        given(ticketTransactionRepository.save(any(TicketTransaction.class)))
            .willReturn(TicketTransaction.builder().build());

        // When
        ticketService.adjustTickets(100L, -3, "Admin deduction");

        // Then
        verify(userTicketRepository).save(any(UserTicket.class));
        verify(ticketTransactionRepository).save(any(TicketTransaction.class));
        verify(paymentMetrics).incrementTicketUsed(3);
    }

    @Test
    @DisplayName("신규 사용자 티켓 생성 - 구독 있음")
    void createUserTicket_WithSubscription() {
        // Given
        given(userTicketRepository.findByUserId(300L)).willReturn(Optional.empty());
        given(subscriptionRepository.findByUserIdAndStatusInOrderByCreatedAtDesc(eq(300L), anyList()))
            .willReturn(List.of(testSubscription));
        
        UserTicket newUserTicket = UserTicket.builder()
            .userId(300L)
            .currentTickets(10)
            .lastTicketRefill(LocalDateTime.now())
            .nextRefillAt(LocalDateTime.now().plusHours(24))
            .build();
        
        given(userTicketRepository.save(any(UserTicket.class))).willReturn(newUserTicket);
        given(ticketTransactionRepository.save(any(TicketTransaction.class)))
            .willReturn(TicketTransaction.builder().build());

        // When
        TicketDto result = ticketService.getUserTickets(300L);

        // Then
        assertThat(result).isNotNull();
        verify(userTicketRepository).save(any(UserTicket.class));
        verify(ticketTransactionRepository).save(any(TicketTransaction.class));
    }

    @Test
    @DisplayName("신규 사용자 티켓 생성 - 구독 없음")
    void createUserTicket_WithoutSubscription() {
        // Given
        given(userTicketRepository.findByUserId(400L)).willReturn(Optional.empty());
        given(subscriptionRepository.findByUserIdAndStatusInOrderByCreatedAtDesc(eq(400L), anyList()))
            .willReturn(Collections.emptyList());
        
        UserTicket newUserTicket = UserTicket.builder()
            .userId(400L)
            .currentTickets(3) // Default initial tickets
            .lastTicketRefill(LocalDateTime.now())
            .nextRefillAt(null)
            .build();
        
        given(userTicketRepository.save(any(UserTicket.class))).willReturn(newUserTicket);
        given(ticketTransactionRepository.save(any(TicketTransaction.class)))
            .willReturn(TicketTransaction.builder().build());

        // When
        TicketDto result = ticketService.getUserTickets(400L);

        // Then
        assertThat(result).isNotNull();
        verify(userTicketRepository).save(any(UserTicket.class));
        verify(ticketTransactionRepository).save(any(TicketTransaction.class));
    }
}
