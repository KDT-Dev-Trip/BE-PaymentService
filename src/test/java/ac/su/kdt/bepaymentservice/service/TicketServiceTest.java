package ac.su.kdt.bepaymentservice.service;

import ac.su.kdt.bepaymentservice.dto.TicketDto;
import ac.su.kdt.bepaymentservice.entity.Subscription;
import ac.su.kdt.bepaymentservice.entity.SubscriptionPlan;
import ac.su.kdt.bepaymentservice.entity.TicketTransaction;
import ac.su.kdt.bepaymentservice.entity.UserTicket;
import ac.su.kdt.bepaymentservice.repository.SubscriptionRepository;
import ac.su.kdt.bepaymentservice.repository.TicketTransactionRepository;
import ac.su.kdt.bepaymentservice.repository.UserTicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

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
    
    @InjectMocks
    private TicketService ticketService;
    
    private UserTicket testUserTicket;
    private SubscriptionPlan testPlan;
    private Subscription testSubscription;
    
    @BeforeEach
    void setUp() {
        testUserTicket = UserTicket.builder()
                .id(1L)
                .userId(1L)
                .currentTickets(5)
                .lastTicketRefill(LocalDateTime.now().minusHours(1))
                .nextRefillAt(LocalDateTime.now().plusHours(23))
                .build();
        
        testPlan = SubscriptionPlan.builder()
                .id(1L)
                .planName("Economy Class")
                .planType(SubscriptionPlan.PlanType.ECONOMY_CLASS)
                .ticketLimit(3)
                .ticketRefillAmount(3)
                .ticketRefillIntervalHours(24)
                .build();
        
        testSubscription = Subscription.builder()
                .id(1L)
                .userId(1L)
                .plan(testPlan)
                .status(Subscription.SubscriptionStatus.ACTIVE)
                .billingCycle(Subscription.BillingCycle.MONTHLY)
                .amount(new BigDecimal("29.00"))
                .build();
    }
    
    @Test
    @DisplayName("존재하는 사용자의 티켓을 정상적으로 조회한다")
    void getUserTickets_ExistingUser_Success() {
        // Given
        given(userTicketRepository.findByUserId(1L))
                .willReturn(Optional.of(testUserTicket));
        
        // When
        TicketDto result = ticketService.getUserTickets(1L);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getCurrentTickets()).isEqualTo(5);
    }
    
    @Test
    @DisplayName("존재하지 않는 사용자의 경우 새로운 티켓 계정을 생성한다")
    void getUserTickets_NewUser_CreatesNewTicketAccount() {
        // Given
        given(userTicketRepository.findByUserId(1L))
                .willReturn(Optional.empty());
        given(subscriptionRepository.findByUserIdAndStatusIn(eq(1L), anyList()))
                .willReturn(Optional.of(testSubscription));
        given(userTicketRepository.save(any(UserTicket.class)))
                .willAnswer(invocation -> {
                    UserTicket userTicket = invocation.getArgument(0);
                    userTicket.setId(1L);
                    userTicket.setCreatedAt(LocalDateTime.now());
                    userTicket.setUpdatedAt(LocalDateTime.now());
                    return userTicket;
                });
        
        // When
        TicketDto result = ticketService.getUserTickets(1L);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getCurrentTickets()).isEqualTo(3); // testPlan.getTicketRefillAmount()
        
        verify(userTicketRepository).save(any(UserTicket.class));
        verify(ticketTransactionRepository).save(any(TicketTransaction.class));
    }
    
    @Test
    @DisplayName("충분한 티켓이 있는 경우 티켓 사용에 성공한다")
    void useTickets_SufficientTickets_Success() {
        // Given
        given(userTicketRepository.findByUserId(1L))
                .willReturn(Optional.of(testUserTicket));
        given(userTicketRepository.save(any(UserTicket.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(ticketTransactionRepository.save(any(TicketTransaction.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        
        // When
        boolean result = ticketService.useTickets(1L, 2, 123L, "Mission attempt");
        
        // Then
        assertThat(result).isTrue();
        assertThat(testUserTicket.getCurrentTickets()).isEqualTo(3); // 5 - 2
        
        verify(userTicketRepository).save(testUserTicket);
        verify(ticketTransactionRepository).save(argThat(transaction ->
                transaction.getTransactionType() == TicketTransaction.TicketTransactionType.SPENT &&
                transaction.getTicketAmount() == -2 &&
                transaction.getBalanceBefore() == 5 &&
                transaction.getBalanceAfter() == 3
        ));
        verify(paymentEventService).publishTicketsUsed(1L, 2, 3);
    }
    
    @Test
    @DisplayName("티켓이 부족한 경우 사용에 실패한다")
    void useTickets_InsufficientTickets_Fails() {
        // Given
        testUserTicket.setCurrentTickets(1);
        given(userTicketRepository.findByUserId(1L))
                .willReturn(Optional.of(testUserTicket));
        
        // When
        boolean result = ticketService.useTickets(1L, 5, 123L, "Mission attempt");
        
        // Then
        assertThat(result).isFalse();
        assertThat(testUserTicket.getCurrentTickets()).isEqualTo(1); // 변경되지 않음
        
        verify(userTicketRepository, never()).save(any(UserTicket.class));
        verify(ticketTransactionRepository, never()).save(any(TicketTransaction.class));
        verify(paymentEventService, never()).publishTicketsUsed(anyLong(), anyInt(), anyInt());
    }
    
    @Test
    @DisplayName("티켓 환불을 정상적으로 처리한다")
    void refundTickets_Success() {
        // Given
        given(userTicketRepository.findByUserId(1L))
                .willReturn(Optional.of(testUserTicket));
        given(userTicketRepository.save(any(UserTicket.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(ticketTransactionRepository.save(any(TicketTransaction.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        
        // When
        ticketService.refundTickets(1L, 2, 123L, "Failed mission");
        
        // Then
        assertThat(testUserTicket.getCurrentTickets()).isEqualTo(7); // 5 + 2
        
        verify(userTicketRepository).save(testUserTicket);
        verify(ticketTransactionRepository).save(argThat(transaction ->
                transaction.getTransactionType() == TicketTransaction.TicketTransactionType.REFUND &&
                transaction.getTicketAmount() == 2 &&
                transaction.getBalanceBefore() == 5 &&
                transaction.getBalanceAfter() == 7
        ));
        verify(paymentEventService).publishTicketsRefunded(1L, 2, 7);
    }
    
    @Test
    @DisplayName("관리자가 티켓을 조정할 수 있다")
    void adjustTickets_PositiveAdjustment_Success() {
        // Given
        given(userTicketRepository.findByUserId(1L))
                .willReturn(Optional.of(testUserTicket));
        given(userTicketRepository.save(any(UserTicket.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(ticketTransactionRepository.save(any(TicketTransaction.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        
        // When
        ticketService.adjustTickets(1L, 3, "Admin bonus");
        
        // Then
        assertThat(testUserTicket.getCurrentTickets()).isEqualTo(8); // 5 + 3
        
        verify(ticketTransactionRepository).save(argThat(transaction ->
                transaction.getTransactionType() == TicketTransaction.TicketTransactionType.ADMIN_ADJUST &&
                transaction.getTicketAmount() == 3 &&
                transaction.getReason().equals("Admin bonus")
        ));
    }
    
    @Test
    @DisplayName("관리자가 티켓을 차감할 수 있다")
    void adjustTickets_NegativeAdjustment_Success() {
        // Given
        given(userTicketRepository.findByUserId(1L))
                .willReturn(Optional.of(testUserTicket));
        given(userTicketRepository.save(any(UserTicket.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(ticketTransactionRepository.save(any(TicketTransaction.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        
        // When
        ticketService.adjustTickets(1L, -2, "Admin penalty");
        
        // Then
        assertThat(testUserTicket.getCurrentTickets()).isEqualTo(3); // 5 - 2
        
        verify(ticketTransactionRepository).save(argThat(transaction ->
                transaction.getTransactionType() == TicketTransaction.TicketTransactionType.ADMIN_ADJUST &&
                transaction.getTicketAmount() == -2 &&
                transaction.getReason().equals("Admin penalty")
        ));
    }
    
    @Test
    @DisplayName("티켓 자동 충전을 처리한다")
    void processTicketRefills_Success() {
        // Given
        UserTicket userTicketForRefill = UserTicket.builder()
                .id(2L)
                .userId(2L)
                .currentTickets(1)
                .nextRefillAt(LocalDateTime.now().minusHours(1)) // 충전 시간이 지남
                .build();
        
        given(userTicketRepository.findUsersEligibleForRefill(any(LocalDateTime.class)))
                .willReturn(List.of(userTicketForRefill));
        given(subscriptionRepository.findByUserIdAndStatusIn(eq(2L), anyList()))
                .willReturn(Optional.of(testSubscription));
        given(userTicketRepository.save(any(UserTicket.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(ticketTransactionRepository.save(any(TicketTransaction.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        
        // When
        ticketService.processTicketRefills();
        
        // Then
        assertThat(userTicketForRefill.getCurrentTickets()).isEqualTo(3); // 1 + 2 (limit에 맞춰서)
        assertThat(userTicketForRefill.getLastTicketRefill()).isNotNull();
        assertThat(userTicketForRefill.getNextRefillAt()).isAfter(LocalDateTime.now());
        
        verify(userTicketRepository).save(userTicketForRefill);
        verify(ticketTransactionRepository).save(argThat(transaction ->
                transaction.getTransactionType() == TicketTransaction.TicketTransactionType.EARNED &&
                transaction.getTicketAmount() == 2 &&
                transaction.getReason().equals("Automatic ticket refill")
        ));
        verify(paymentEventService).publishTicketsRefilled(2L, 2, 3);
    }
    
    @Test
    @DisplayName("이미 티켓 한도에 도달한 사용자는 충전되지 않는다")
    void processTicketRefills_AlreadyAtLimit_SkipsRefill() {
        // Given
        UserTicket userTicketAtLimit = UserTicket.builder()
                .id(2L)
                .userId(2L)
                .currentTickets(3) // 이미 한도에 도달
                .nextRefillAt(LocalDateTime.now().minusHours(1))
                .build();
        
        given(userTicketRepository.findUsersEligibleForRefill(any(LocalDateTime.class)))
                .willReturn(List.of(userTicketAtLimit));
        given(subscriptionRepository.findByUserIdAndStatusIn(eq(2L), anyList()))
                .willReturn(Optional.of(testSubscription));
        given(userTicketRepository.save(any(UserTicket.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        
        // When
        ticketService.processTicketRefills();
        
        // Then
        assertThat(userTicketAtLimit.getCurrentTickets()).isEqualTo(3); // 변경 없음
        assertThat(userTicketAtLimit.getNextRefillAt()).isAfter(LocalDateTime.now()); // 다음 충전 시간만 업데이트
        
        // 한도에 도달한 경우 nextRefillAt 업데이트를 위해 save가 호출됨
        verify(userTicketRepository).save(userTicketAtLimit);
        verify(ticketTransactionRepository, never()).save(any(TicketTransaction.class));
        verify(paymentEventService, never()).publishTicketsRefilled(anyLong(), anyInt(), anyInt());
    }
    
    @Test
    @DisplayName("활성 구독이 없는 사용자는 충전되지 않는다")
    void processTicketRefills_NoActiveSubscription_SkipsRefill() {
        // Given
        UserTicket userTicketWithoutSubscription = UserTicket.builder()
                .id(2L)
                .userId(2L)
                .currentTickets(1)
                .nextRefillAt(LocalDateTime.now().minusHours(1))
                .build();
        
        given(userTicketRepository.findUsersEligibleForRefill(any(LocalDateTime.class)))
                .willReturn(List.of(userTicketWithoutSubscription));
        given(subscriptionRepository.findByUserIdAndStatusIn(eq(2L), anyList()))
                .willReturn(Optional.empty());
        
        // When
        ticketService.processTicketRefills();
        
        // Then
        assertThat(userTicketWithoutSubscription.getCurrentTickets()).isEqualTo(1); // 변경 없음
        
        verify(userTicketRepository, never()).save(any(UserTicket.class));
        verify(ticketTransactionRepository, never()).save(any(TicketTransaction.class));
        verify(paymentEventService, never()).publishTicketsRefilled(anyLong(), anyInt(), anyInt());
    }
}