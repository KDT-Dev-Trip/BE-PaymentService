package ac.su.kdt.bepaymentservice.service;

import ac.su.kdt.bepaymentservice.dto.CreateSubscriptionRequest;
import ac.su.kdt.bepaymentservice.dto.SubscriptionDto;
import ac.su.kdt.bepaymentservice.entity.Subscription;
import ac.su.kdt.bepaymentservice.entity.SubscriptionPlan;
import ac.su.kdt.bepaymentservice.repository.SubscriptionPlanRepository;
import ac.su.kdt.bepaymentservice.repository.SubscriptionRepository;
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
@DisplayName("SubscriptionService 단위 테스트")
class SubscriptionServiceTest {
    
    @Mock
    private SubscriptionRepository subscriptionRepository;
    
    @Mock
    private SubscriptionPlanRepository subscriptionPlanRepository;
    
    
    @Mock
    private PaymentEventService paymentEventService;
    
    @Mock
    private ac.su.kdt.bepaymentservice.metrics.PaymentMetrics paymentMetrics;
    
    @InjectMocks
    private SubscriptionService subscriptionService;
    
    private SubscriptionPlan testPlan;
    private CreateSubscriptionRequest createRequest;
    
    @BeforeEach
    void setUp() {
        testPlan = SubscriptionPlan.builder()
                .id(1L)
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
        
        createRequest = CreateSubscriptionRequest.builder()
                .userId(1L)
                .planId(1L)
                .billingCycle(Subscription.BillingCycle.MONTHLY)
                .build();
    }
    
    @Test
    @DisplayName("새로운 구독을 성공적으로 생성한다")
    void createSubscription_Success() {
        // Given
        given(subscriptionRepository.findByUserIdAndStatusIn(eq(1L), anyList()))
                .willReturn(Optional.empty());
        given(subscriptionPlanRepository.findById(1L))
                .willReturn(Optional.of(testPlan));
        given(subscriptionRepository.save(any(Subscription.class)))
                .willAnswer(invocation -> {
                    Subscription subscription = invocation.getArgument(0);
                    subscription.setId(1L);
                    subscription.setCreatedAt(LocalDateTime.now());
                    subscription.setUpdatedAt(LocalDateTime.now());
                    return subscription;
                });
        
        // When
        SubscriptionDto result = subscriptionService.createSubscription(createRequest);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getPlan().getId()).isEqualTo(1L);
        assertThat(result.getBillingCycle()).isEqualTo(Subscription.BillingCycle.MONTHLY);
        assertThat(result.getAmount()).isEqualTo(new BigDecimal("29.00"));
        assertThat(result.getStatus()).isEqualTo(Subscription.SubscriptionStatus.INCOMPLETE);
        
        verify(subscriptionRepository).save(any(Subscription.class));
        verify(paymentEventService).publishSubscriptionCreated(any(Subscription.class));
    }
    
    @Test
    @DisplayName("이미 활성 구독이 있는 경우 예외를 발생시킨다")
    void createSubscription_AlreadyHasActiveSubscription_ThrowsException() {
        // Given
        Subscription existingSubscription = Subscription.builder()
                .id(1L)
                .userId(1L)
                .status(Subscription.SubscriptionStatus.ACTIVE)
                .build();
        
        given(subscriptionRepository.findByUserIdAndStatusIn(eq(1L), anyList()))
                .willReturn(Optional.of(existingSubscription));
        
        // When & Then
        assertThatThrownBy(() -> subscriptionService.createSubscription(createRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("User already has an active subscription");
        
        verify(subscriptionRepository, never()).save(any(Subscription.class));
        verify(paymentEventService, never()).publishSubscriptionCreated(any(Subscription.class));
    }
    
    @Test
    @DisplayName("존재하지 않는 플랜 ID로 구독 생성 시 예외를 발생시킨다")
    void createSubscription_PlanNotFound_ThrowsException() {
        // Given
        given(subscriptionRepository.findByUserIdAndStatusIn(eq(1L), anyList()))
                .willReturn(Optional.empty());
        given(subscriptionPlanRepository.findById(1L))
                .willReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> subscriptionService.createSubscription(createRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Subscription plan not found");
    }
    
    @Test
    @DisplayName("비활성 플랜으로 구독 생성 시 예외를 발생시킨다")
    void createSubscription_InactivePlan_ThrowsException() {
        // Given
        testPlan.setIsActive(false);
        
        given(subscriptionRepository.findByUserIdAndStatusIn(eq(1L), anyList()))
                .willReturn(Optional.empty());
        given(subscriptionPlanRepository.findById(1L))
                .willReturn(Optional.of(testPlan));
        
        // When & Then
        assertThatThrownBy(() -> subscriptionService.createSubscription(createRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Subscription plan is not active");
    }
    
    @Test
    @DisplayName("연간 결제 주기로 구독 생성 시 연간 가격을 적용한다")
    void createSubscription_YearlyBilling_UsesYearlyPrice() {
        // Given
        createRequest.setBillingCycle(Subscription.BillingCycle.YEARLY);
        
        given(subscriptionRepository.findByUserIdAndStatusIn(eq(1L), anyList()))
                .willReturn(Optional.empty());
        given(subscriptionPlanRepository.findById(1L))
                .willReturn(Optional.of(testPlan));
        given(subscriptionRepository.save(any(Subscription.class)))
                .willAnswer(invocation -> {
                    Subscription subscription = invocation.getArgument(0);
                    subscription.setId(1L);
                    subscription.setCreatedAt(LocalDateTime.now());
                    subscription.setUpdatedAt(LocalDateTime.now());
                    return subscription;
                });
        
        // When
        SubscriptionDto result = subscriptionService.createSubscription(createRequest);
        
        // Then
        assertThat(result.getAmount()).isEqualTo(new BigDecimal("290.00"));
        assertThat(result.getBillingCycle()).isEqualTo(Subscription.BillingCycle.YEARLY);
    }
    
    @Test
    @DisplayName("트라이얼로 구독 생성 시 트라이얼 상태와 날짜를 설정한다")
    void createSubscription_WithTrial_SetsTrialStatus() {
        // Given
        createRequest.setStartTrial(true);
        createRequest.setTrialDays(7);
        
        given(subscriptionRepository.findByUserIdAndStatusIn(eq(1L), anyList()))
                .willReturn(Optional.empty());
        given(subscriptionPlanRepository.findById(1L))
                .willReturn(Optional.of(testPlan));
        given(subscriptionRepository.save(any(Subscription.class)))
                .willAnswer(invocation -> {
                    Subscription subscription = invocation.getArgument(0);
                    subscription.setId(1L);
                    subscription.setCreatedAt(LocalDateTime.now());
                    subscription.setUpdatedAt(LocalDateTime.now());
                    return subscription;
                });
        
        // When
        SubscriptionDto result = subscriptionService.createSubscription(createRequest);
        
        // Then
        assertThat(result.getStatus()).isEqualTo(Subscription.SubscriptionStatus.TRIAL);
        assertThat(result.getTrialStart()).isNotNull();
        assertThat(result.getTrialEnd()).isNotNull();
        assertThat(result.getTrialEnd()).isAfter(result.getTrialStart());
    }
    
    @Test
    @DisplayName("사용자의 활성 구독을 정상적으로 조회한다")
    void getUserActiveSubscription_Success() {
        // Given
        Subscription activeSubscription = Subscription.builder()
                .id(1L)
                .userId(1L)
                .plan(testPlan)
                .status(Subscription.SubscriptionStatus.ACTIVE)
                .billingCycle(Subscription.BillingCycle.MONTHLY)
                .amount(new BigDecimal("29.00"))
                .currency("KRW")
                .build();
        
        given(subscriptionRepository.findByUserIdAndStatusIn(eq(1L), anyList()))
                .willReturn(Optional.of(activeSubscription));
        
        // When
        SubscriptionDto result = subscriptionService.getUserActiveSubscription(1L);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo(Subscription.SubscriptionStatus.ACTIVE);
    }
    
    @Test
    @DisplayName("활성 구독이 없는 경우 null을 반환한다")
    void getUserActiveSubscription_NoActiveSubscription_ReturnsNull() {
        // Given
        given(subscriptionRepository.findByUserIdAndStatusIn(eq(1L), anyList()))
                .willReturn(Optional.empty());
        
        // When
        SubscriptionDto result = subscriptionService.getUserActiveSubscription(1L);
        
        // Then
        assertThat(result).isNull();
    }
    
    @Test
    @DisplayName("구독을 기간 종료 시 취소로 설정한다")
    void cancelSubscription_CancelAtPeriodEnd_Success() {
        // Given
        Subscription subscription = Subscription.builder()
                .id(1L)
                .userId(1L)
                .plan(testPlan)
                .status(Subscription.SubscriptionStatus.ACTIVE)
                .cancelAtPeriodEnd(false)
                .build();
        
        given(subscriptionRepository.findById(1L))
                .willReturn(Optional.of(subscription));
        given(subscriptionRepository.save(any(Subscription.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        
        // When
        SubscriptionDto result = subscriptionService.cancelSubscription(1L, true);
        
        // Then
        assertThat(result.getCancelAtPeriodEnd()).isTrue();
        assertThat(result.getStatus()).isEqualTo(Subscription.SubscriptionStatus.ACTIVE);
        
        verify(paymentEventService).publishSubscriptionCancelled(any(Subscription.class));
    }
    
    @Test
    @DisplayName("구독을 즉시 취소한다")
    void cancelSubscription_ImmediateCancel_Success() {
        // Given
        Subscription subscription = Subscription.builder()
                .id(1L)
                .userId(1L)
                .plan(testPlan)
                .status(Subscription.SubscriptionStatus.ACTIVE)
                .build();
        
        given(subscriptionRepository.findById(1L))
                .willReturn(Optional.of(subscription));
        given(subscriptionRepository.save(any(Subscription.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        
        // When
        SubscriptionDto result = subscriptionService.cancelSubscription(1L, false);
        
        // Then
        assertThat(result.getStatus()).isEqualTo(Subscription.SubscriptionStatus.CANCELED);
        assertThat(result.getCanceledAt()).isNotNull();
        
        verify(paymentEventService).publishSubscriptionCancelled(any(Subscription.class));
    }
    
    @Test
    @DisplayName("존재하지 않는 구독 취소 시 예외를 발생시킨다")
    void cancelSubscription_NotFound_ThrowsException() {
        // Given
        given(subscriptionRepository.findById(1L))
                .willReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> subscriptionService.cancelSubscription(1L, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Subscription not found");
    }
    
    @Test
    @DisplayName("만료된 구독들을 처리한다")
    void processExpiredSubscriptions_Success() {
        // Given
        Subscription expiredSubscription = Subscription.builder()
                .id(1L)
                .userId(1L)
                .status(Subscription.SubscriptionStatus.ACTIVE)
                .currentPeriodEnd(LocalDateTime.now().minusDays(1))
                .build();
        
        given(subscriptionRepository.findExpiredSubscriptions(
                eq(Subscription.SubscriptionStatus.ACTIVE), any(LocalDateTime.class)))
                .willReturn(List.of(expiredSubscription));
        given(subscriptionRepository.save(any(Subscription.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        
        // When
        subscriptionService.processExpiredSubscriptions();
        
        // Then
        verify(subscriptionRepository).save(argThat(subscription -> 
                subscription.getStatus() == Subscription.SubscriptionStatus.EXPIRED));
        verify(paymentEventService).publishSubscriptionExpired(any(Subscription.class));
    }
}