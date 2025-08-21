package ac.su.kdt.bepaymentservice.repository;

import ac.su.kdt.bepaymentservice.entity.Subscription;
import ac.su.kdt.bepaymentservice.entity.SubscriptionPlan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@DisplayName("SubscriptionRepository 테스트")
class SubscriptionRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private SubscriptionRepository subscriptionRepository;
    
    private SubscriptionPlan testPlan;
    private Subscription testSubscription;
    
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
        
        testPlan = entityManager.persistAndFlush(testPlan);
        
        testSubscription = Subscription.builder()
                .userId(1L)
                .plan(testPlan)
                .status(Subscription.SubscriptionStatus.ACTIVE)
                .billingCycle(Subscription.BillingCycle.MONTHLY)
                .amount(new BigDecimal("29.00"))
                .currency("KRW")
                .stripeSubscriptionId("sub_test123")
                .stripeCustomerId("cus_test123")
                .currentPeriodStart(LocalDateTime.now().minusDays(5))
                .currentPeriodEnd(LocalDateTime.now().plusDays(25))
                .autoRenewal(true)
                .build();
        
        testSubscription = entityManager.persistAndFlush(testSubscription);
        entityManager.clear();
    }
    
    @Test
    @DisplayName("사용자 ID와 상태로 구독을 조회할 수 있다")
    void findByUserIdAndStatus_Success() {
        // When
        Optional<Subscription> result = subscriptionRepository.findByUserIdAndStatus(1L, Subscription.SubscriptionStatus.ACTIVE);
        
        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getUserId()).isEqualTo(1L);
        assertThat(result.get().getStatus()).isEqualTo(Subscription.SubscriptionStatus.ACTIVE);
        assertThat(result.get().getPlan().getPlanName()).isEqualTo("Economy Class");
    }
    
    @Test
    @DisplayName("사용자 ID와 여러 상태로 구독을 조회할 수 있다")
    void findByUserIdAndStatusInOrderByCreatedAtDesc_Success() {
        // Given
        List<Subscription.SubscriptionStatus> statuses = List.of(
                Subscription.SubscriptionStatus.ACTIVE,
                Subscription.SubscriptionStatus.TRIAL
        );
        
        // When
        List<Subscription> result = subscriptionRepository.findByUserIdAndStatusInOrderByCreatedAtDesc(1L, statuses);
        
        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(1L);
        assertThat(result.get(0).getStatus()).isEqualTo(Subscription.SubscriptionStatus.ACTIVE);
    }
    
    @Test
    @DisplayName("Stripe 구독 ID로 구독을 조회할 수 있다")
    void findByStripeSubscriptionId_Success() {
        // When
        Optional<Subscription> result = subscriptionRepository.findByStripeSubscriptionId("sub_test123");
        
        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getStripeSubscriptionId()).isEqualTo("sub_test123");
        assertThat(result.get().getUserId()).isEqualTo(1L);
    }
    
    @Test
    @DisplayName("존재하지 않는 Stripe 구독 ID로 조회 시 빈 결과를 반환한다")
    void findByStripeSubscriptionId_NotFound_ReturnsEmpty() {
        // When
        Optional<Subscription> result = subscriptionRepository.findByStripeSubscriptionId("sub_nonexistent");
        
        // Then
        assertThat(result).isEmpty();
    }
    
    @Test
    @DisplayName("만료될 구독들을 조회할 수 있다")
    void findExpiringSubscriptions_Success() {
        // Given
        LocalDateTime expiryDate = LocalDateTime.now().plusDays(30);
        
        // When
        List<Subscription> result = subscriptionRepository.findExpiringSubscriptions(expiryDate, Subscription.SubscriptionStatus.ACTIVE);
        
        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCurrentPeriodEnd()).isBefore(expiryDate);
        assertThat(result.get(0).getStatus()).isEqualTo(Subscription.SubscriptionStatus.ACTIVE);
    }
    
    @Test
    @DisplayName("이미 만료된 구독들을 조회할 수 있다")
    void findExpiredSubscriptions_Success() {
        // Given - 이미 만료된 구독 생성
        Subscription expiredSubscription = Subscription.builder()
                .userId(2L)
                .plan(testPlan)
                .status(Subscription.SubscriptionStatus.ACTIVE)
                .billingCycle(Subscription.BillingCycle.MONTHLY)
                .amount(new BigDecimal("29.00"))
                .currency("KRW")
                .currentPeriodStart(LocalDateTime.now().minusDays(35))
                .currentPeriodEnd(LocalDateTime.now().minusDays(5)) // 이미 만료됨
                .build();
        
        entityManager.persistAndFlush(expiredSubscription);
        entityManager.clear();
        
        LocalDateTime now = LocalDateTime.now();
        
        // When
        List<Subscription> result = subscriptionRepository.findExpiredSubscriptions(Subscription.SubscriptionStatus.ACTIVE, now);
        
        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(2L);
        assertThat(result.get(0).getCurrentPeriodEnd()).isBefore(now);
    }
    
    @Test
    @DisplayName("Stripe 고객 ID로 구독들을 조회할 수 있다")
    void findByStripeCustomerId_Success() {
        // When
        List<Subscription> result = subscriptionRepository.findByStripeCustomerId("cus_test123");
        
        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStripeCustomerId()).isEqualTo("cus_test123");
        assertThat(result.get(0).getUserId()).isEqualTo(1L);
    }
    
    @Test
    @DisplayName("플랜별 활성 구독 수를 조회할 수 있다")
    void countByPlanIdAndStatus_Success() {
        // Given - 같은 플랜으로 추가 구독 생성
        Subscription anotherSubscription = Subscription.builder()
                .userId(2L)
                .plan(testPlan)
                .status(Subscription.SubscriptionStatus.ACTIVE)
                .billingCycle(Subscription.BillingCycle.YEARLY)
                .amount(new BigDecimal("290.00"))
                .currency("KRW")
                .build();
        
        entityManager.persistAndFlush(anotherSubscription);
        entityManager.clear();
        
        // When
        Long count = subscriptionRepository.countByPlanIdAndStatus(testPlan.getId(), Subscription.SubscriptionStatus.ACTIVE);
        
        // Then
        assertThat(count).isEqualTo(2L);
    }
    
    @Test
    @DisplayName("사용자의 구독을 생성일 역순으로 조회할 수 있다")
    void findByUserIdOrderByCreatedAtDesc_Success() {
        // Given - testSubscription은 이미 setUp에서 persist됨 (더 오래된 것)
        // 최신 구독을 나중에 persist - 이는 testSubscription보다 더 늦은 시간에 생성되므로 더 최신이 됨
        Subscription newerSubscription = Subscription.builder()
                .userId(1L)
                .plan(testPlan)
                .status(Subscription.SubscriptionStatus.CANCELED)
                .billingCycle(Subscription.BillingCycle.MONTHLY)
                .amount(new BigDecimal("29.00"))
                .currency("KRW")
                .canceledAt(LocalDateTime.now().minusMonths(1))
                .build();
        
        entityManager.persist(newerSubscription);
        entityManager.flush();
        entityManager.clear();
        
        // When
        List<Subscription> result = subscriptionRepository.findByUserIdOrderByCreatedAtDesc(1L);
        
        // Then
        assertThat(result).hasSize(2);
        
        // 최신 구독이 먼저 와야 함 (DESC order) - newerSubscription이 더 최신이므로 먼저 온다
        assertThat(result.get(0).getStatus()).isEqualTo(Subscription.SubscriptionStatus.CANCELED);
        assertThat(result.get(1).getStatus()).isEqualTo(Subscription.SubscriptionStatus.ACTIVE);
    }
    
    @Test
    @DisplayName("팀 ID와 상태로 구독들을 조회할 수 있다")
    void findByTeamIdAndStatus_Success() {
        // Given - 팀 구독 생성
        Subscription teamSubscription = Subscription.builder()
                .userId(3L)
                .teamId(100L)
                .plan(testPlan)
                .status(Subscription.SubscriptionStatus.ACTIVE)
                .billingCycle(Subscription.BillingCycle.MONTHLY)
                .amount(new BigDecimal("29.00"))
                .currency("KRW")
                .build();
        
        entityManager.persistAndFlush(teamSubscription);
        entityManager.clear();
        
        // When
        List<Subscription> result = subscriptionRepository.findByTeamIdAndStatus(100L, Subscription.SubscriptionStatus.ACTIVE);
        
        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTeamId()).isEqualTo(100L);
        assertThat(result.get(0).getUserId()).isEqualTo(3L);
        assertThat(result.get(0).getStatus()).isEqualTo(Subscription.SubscriptionStatus.ACTIVE);
    }
    
    @Test
    @DisplayName("존재하지 않는 팀 ID로 조회 시 빈 결과를 반환한다")
    void findByTeamIdAndStatus_NotFound_ReturnsEmpty() {
        // When
        List<Subscription> result = subscriptionRepository.findByTeamIdAndStatus(999L, Subscription.SubscriptionStatus.ACTIVE);
        
        // Then
        assertThat(result).isEmpty();
    }
}