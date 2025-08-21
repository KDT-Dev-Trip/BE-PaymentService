package ac.su.kdt.bepaymentservice.integration;

import ac.su.kdt.bepaymentservice.dto.CreateSubscriptionRequest;
import ac.su.kdt.bepaymentservice.dto.SubscriptionDto;
import ac.su.kdt.bepaymentservice.dto.TicketDto;
import ac.su.kdt.bepaymentservice.entity.Subscription;
import ac.su.kdt.bepaymentservice.entity.SubscriptionPlan;
import ac.su.kdt.bepaymentservice.repository.SubscriptionPlanRepository;
import ac.su.kdt.bepaymentservice.service.SubscriptionService;
import ac.su.kdt.bepaymentservice.service.TicketService;
import ac.su.kdt.bepaymentservice.metrics.PaymentMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("결제 서비스 통합 테스트")
class PaymentServiceIntegrationTest {
    
    @MockBean
    private PaymentMetrics paymentMetrics;
    
    @Autowired
    private SubscriptionService subscriptionService;
    
    @Autowired
    private TicketService ticketService;
    
    @Autowired
    private SubscriptionPlanRepository subscriptionPlanRepository;
    
    private SubscriptionPlan testPlan;
    
    @BeforeEach
    void setUp() {
        testPlan = SubscriptionPlan.builder()
                .planName("Economy Class")
                .planType(SubscriptionPlan.PlanType.ECONOMY_CLASS)
                .monthlyPrice(new BigDecimal("29.00"))
                .yearlyPrice(new BigDecimal("290.00"))
                .maxTeamMembers(2)
                .maxMonthlyAttempts(10)
                .ticketLimit(3)
                .ticketRefillAmount(3)
                .ticketRefillIntervalHours(24)
                .isActive(true)
                .build();
        
        testPlan = subscriptionPlanRepository.save(testPlan);
    }
    
    @Test
    @DisplayName("구독 생성부터 티켓 사용까지 전체 플로우가 정상 동작한다")
    void fullPaymentFlow_Success() {
        // Given
        Long userId = 1L;
        CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
                .userId(userId)
                .planId(testPlan.getId())
                .billingCycle(Subscription.BillingCycle.MONTHLY)
                .build();
        
        // When - 구독 생성
        SubscriptionDto subscription = subscriptionService.createSubscription(request);
        
        // Then - 구독이 성공적으로 생성됨
        assertThat(subscription).isNotNull();
        assertThat(subscription.getUserId()).isEqualTo(userId);
        assertThat(subscription.getPlan().getId()).isEqualTo(testPlan.getId());
        assertThat(subscription.getAmount()).isEqualTo(new BigDecimal("29.00"));
        
        // When - 구독 활성화 (실제로는 결제 완료 시 발생)
        subscription = subscriptionService.activateSubscription(subscription.getId());
        
        // When - 사용자 티켓 조회 (새 사용자이므로 티켓 계정이 생성됨)
        TicketDto userTickets = ticketService.getUserTickets(userId);
        
        // Then - 플랜에 따른 초기 티켓이 지급됨
        assertThat(userTickets).isNotNull();
        assertThat(userTickets.getUserId()).isEqualTo(userId);
        assertThat(userTickets.getCurrentTickets()).isEqualTo(3); // testPlan.getTicketRefillAmount()
        
        // When - 티켓 사용
        boolean useResult = ticketService.useTickets(userId, 2, 123L, "Test mission");
        
        // Then - 티켓 사용 성공
        assertThat(useResult).isTrue();
        
        // When - 티켓 잔액 확인
        TicketDto updatedTickets = ticketService.getUserTickets(userId);
        
        // Then - 티켓이 차감됨
        assertThat(updatedTickets.getCurrentTickets()).isEqualTo(1); // 3 - 2
        
        // When - 티켓 환불
        ticketService.refundTickets(userId, 1, 123L, "Test refund");
        
        // When - 최종 티켓 잔액 확인
        TicketDto finalTickets = ticketService.getUserTickets(userId);
        
        // Then - 티켓이 환불됨
        assertThat(finalTickets.getCurrentTickets()).isEqualTo(2); // 1 + 1
    }
    
    @Test
    @DisplayName("구독 취소 후에도 티켓 시스템이 정상 동작한다")
    void subscriptionCancelledFlow_Success() {
        // Given
        Long userId = 2L;
        CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
                .userId(userId)
                .planId(testPlan.getId())
                .billingCycle(Subscription.BillingCycle.MONTHLY)
                .build();
        
        // When - 구독 생성 및 활성화
        SubscriptionDto subscription = subscriptionService.createSubscription(request);
        subscription = subscriptionService.activateSubscription(subscription.getId());
        
        // When - 활성화된 상태에서 먼저 티켓 조회 (티켓 계정 생성)
        TicketDto ticketsBeforeCancellation = ticketService.getUserTickets(userId);
        assertThat(ticketsBeforeCancellation.getCurrentTickets()).isEqualTo(3);
        
        // When - 구독 취소
        SubscriptionDto cancelledSubscription = subscriptionService.cancelSubscription(subscription.getId(), false);
        
        // Then - 구독이 취소됨
        assertThat(cancelledSubscription.getStatus()).isEqualTo(Subscription.SubscriptionStatus.CANCELED);
        assertThat(cancelledSubscription.getCanceledAt()).isNotNull();
        
        // When - 취소된 구독 상태에서 티켓 조회
        TicketDto userTickets = ticketService.getUserTickets(userId);
        
        // Then - 티켓 시스템은 여전히 동작함 (기존 티켓 유지)
        assertThat(userTickets).isNotNull();
        assertThat(userTickets.getUserId()).isEqualTo(userId);
        assertThat(userTickets.getCurrentTickets()).isEqualTo(3); // 활성화시 부여된 초기 티켓이 유지됨
        
        // When - 취소된 구독 상태에서 티켓 사용 시도
        boolean useResult = ticketService.useTickets(userId, 1, 124L, "Test after cancellation");
        
        // Then - 기존 티켓은 여전히 사용 가능
        assertThat(useResult).isTrue();
    }
    
    @Test
    @DisplayName("여러 사용자의 구독과 티켓이 독립적으로 관리된다")
    void multipleUsersFlow_Success() {
        // Given
        Long user1Id = 3L;
        Long user2Id = 4L;
        
        CreateSubscriptionRequest request1 = CreateSubscriptionRequest.builder()
                .userId(user1Id)
                .planId(testPlan.getId())
                .billingCycle(Subscription.BillingCycle.MONTHLY)
                .build();
        
        CreateSubscriptionRequest request2 = CreateSubscriptionRequest.builder()
                .userId(user2Id)
                .planId(testPlan.getId())
                .billingCycle(Subscription.BillingCycle.YEARLY)
                .build();
        
        // When - 두 사용자의 구독 생성 및 활성화
        SubscriptionDto subscription1 = subscriptionService.createSubscription(request1);
        subscription1 = subscriptionService.activateSubscription(subscription1.getId());
        
        SubscriptionDto subscription2 = subscriptionService.createSubscription(request2);
        subscription2 = subscriptionService.activateSubscription(subscription2.getId());
        
        // Then - 각각 독립적인 구독이 생성됨
        assertThat(subscription1.getUserId()).isEqualTo(user1Id);
        assertThat(subscription1.getBillingCycle()).isEqualTo(Subscription.BillingCycle.MONTHLY);
        assertThat(subscription1.getAmount()).isEqualTo(new BigDecimal("29.00"));
        
        assertThat(subscription2.getUserId()).isEqualTo(user2Id);
        assertThat(subscription2.getBillingCycle()).isEqualTo(Subscription.BillingCycle.YEARLY);
        assertThat(subscription2.getAmount()).isEqualTo(new BigDecimal("290.00"));
        
        // When - 각 사용자의 티켓 사용
        ticketService.useTickets(user1Id, 1, 201L, "User 1 mission");
        ticketService.useTickets(user2Id, 2, 202L, "User 2 mission");
        
        // Then - 각 사용자의 티켓이 독립적으로 차감됨
        TicketDto user1Tickets = ticketService.getUserTickets(user1Id);
        TicketDto user2Tickets = ticketService.getUserTickets(user2Id);
        
        assertThat(user1Tickets.getCurrentTickets()).isEqualTo(2); // 3 - 1
        assertThat(user2Tickets.getCurrentTickets()).isEqualTo(1); // 3 - 2
        
        // When - 한 사용자의 구독 취소
        subscriptionService.cancelSubscription(subscription1.getId(), true);
        
        // Then - 다른 사용자의 구독은 영향받지 않음
        SubscriptionDto activeSubscription2 = subscriptionService.getUserActiveSubscription(user2Id);
        assertThat(activeSubscription2).isNotNull();
        assertThat(activeSubscription2.getStatus()).isEqualTo(Subscription.SubscriptionStatus.ACTIVE);
    }
    
    @Test
    @DisplayName("티켓 부족 시 사용이 실패하고 잔액이 변경되지 않는다")
    void insufficientTicketsFlow_Success() {
        // Given
        Long userId = 5L;
        CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
                .userId(userId)
                .planId(testPlan.getId())
                .billingCycle(Subscription.BillingCycle.MONTHLY)
                .build();
        
        SubscriptionDto subscription = subscriptionService.createSubscription(request);
        subscriptionService.activateSubscription(subscription.getId());
        
        // When - 보유한 티켓보다 많은 티켓 사용 시도
        TicketDto initialTickets = ticketService.getUserTickets(userId);
        boolean useResult = ticketService.useTickets(userId, 10, 301L, "Excessive ticket use");
        
        // Then - 티켓 사용 실패
        assertThat(useResult).isFalse();
        
        // When - 티켓 잔액 확인
        TicketDto finalTickets = ticketService.getUserTickets(userId);
        
        // Then - 티켓 잔액이 변경되지 않음
        assertThat(finalTickets.getCurrentTickets()).isEqualTo(initialTickets.getCurrentTickets());
    }
}