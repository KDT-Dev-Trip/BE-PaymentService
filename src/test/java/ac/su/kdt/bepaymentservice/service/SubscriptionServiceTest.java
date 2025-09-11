package ac.su.kdt.bepaymentservice.service;

import ac.su.kdt.bepaymentservice.dto.CreateSubscriptionRequest;
import ac.su.kdt.bepaymentservice.dto.SubscriptionDto;
import ac.su.kdt.bepaymentservice.entity.Subscription;
import ac.su.kdt.bepaymentservice.entity.SubscriptionPlan;
import ac.su.kdt.bepaymentservice.repository.SubscriptionPlanRepository;
import ac.su.kdt.bepaymentservice.repository.SubscriptionRepository;
import ac.su.kdt.bepaymentservice.metrics.PaymentMetrics;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

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
    private PaymentMetrics paymentMetrics;

    @Mock
    private PaymentEventPublisher paymentEventPublisher;

    @InjectMocks
    private SubscriptionService subscriptionService;

    private SubscriptionPlan testPlan;
    private Subscription testSubscription;
    private CreateSubscriptionRequest testRequest;
    private Timer.Sample mockTimer;

    @BeforeEach
    void setUp() {
        mockTimer = Timer.start();
        
        testPlan = SubscriptionPlan.builder()
            .id(1L)
            .planName("BASIC")
            .monthlyPrice(new BigDecimal("19000"))
            .yearlyPrice(new BigDecimal("190000"))
            .isActive(true)
            .ticketLimit(50)
            .ticketRefillAmount(10)
            .ticketRefillIntervalHours(24)
            .build();

        testSubscription = Subscription.builder()
            .id(1L)
            .userId(100L)
            .teamId(200L)
            .plan(testPlan)
            .status(Subscription.SubscriptionStatus.ACTIVE)
            .billingCycle(Subscription.BillingCycle.MONTHLY)
            .amount(new BigDecimal("19000"))
            .currency("KRW")
            .autoRenewal(true)
            .currentPeriodStart(LocalDateTime.now())
            .currentPeriodEnd(LocalDateTime.now().plusMonths(1))
            .build();

        testRequest = CreateSubscriptionRequest.builder()
            .userId(100L)
            .teamId(200L)
            .planId(1L)
            .billingCycle(Subscription.BillingCycle.MONTHLY)
            .startTrial(false)
            .build();
    }

    @Test
    @DisplayName("구독 생성 - 성공 (월간 결제)")
    void createSubscription_Monthly_Success() {
        // Given
        given(paymentMetrics.startSubscriptionTimer()).willReturn(mockTimer);
        given(subscriptionRepository.findByUserIdAndStatusInOrderByCreatedAtDesc(eq(100L), anyList()))
            .willReturn(Collections.emptyList());
        given(subscriptionPlanRepository.findById(1L)).willReturn(Optional.of(testPlan));
        given(subscriptionRepository.save(any(Subscription.class))).willReturn(testSubscription);

        // When
        SubscriptionDto result = subscriptionService.createSubscription(testRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(100L);
        assertThat(result.getAmount()).isEqualTo(new BigDecimal("19000"));
        assertThat(result.getBillingCycle()).isEqualTo(Subscription.BillingCycle.MONTHLY);
        
        verify(subscriptionRepository).save(any(Subscription.class));
        verify(paymentEventService).publishSubscriptionCreated(any(Subscription.class));
        verify(paymentMetrics).incrementSubscriptionSuccess();
        verify(paymentMetrics).recordSubscriptionProcessingTime(mockTimer);
    }

    @Test
    @DisplayName("구독 생성 - 성공 (연간 결제)")
    void createSubscription_Yearly_Success() {
        // Given
        testRequest.setBillingCycle(Subscription.BillingCycle.YEARLY);
        Subscription yearlySubscription = testSubscription.toBuilder()
            .billingCycle(Subscription.BillingCycle.YEARLY)
            .amount(new BigDecimal("190000"))
            .build();

        given(paymentMetrics.startSubscriptionTimer()).willReturn(mockTimer);
        given(subscriptionRepository.findByUserIdAndStatusInOrderByCreatedAtDesc(eq(100L), anyList()))
            .willReturn(Collections.emptyList());
        given(subscriptionPlanRepository.findById(1L)).willReturn(Optional.of(testPlan));
        given(subscriptionRepository.save(any(Subscription.class))).willReturn(yearlySubscription);

        // When
        SubscriptionDto result = subscriptionService.createSubscription(testRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAmount()).isEqualTo(new BigDecimal("190000"));
        assertThat(result.getBillingCycle()).isEqualTo(Subscription.BillingCycle.YEARLY);
        
        verify(subscriptionRepository).save(any(Subscription.class));
        verify(paymentEventService).publishSubscriptionCreated(any(Subscription.class));
    }

    @Test
    @DisplayName("구독 생성 - 트라이얼 모드")
    void createSubscription_WithTrial_Success() {
        // Given
        testRequest.setStartTrial(true);
        testRequest.setTrialDays(7);
        
        Subscription trialSubscription = testSubscription.toBuilder()
            .status(Subscription.SubscriptionStatus.TRIAL)
            .trialStart(LocalDateTime.now())
            .trialEnd(LocalDateTime.now().plusDays(7))
            .build();

        given(paymentMetrics.startSubscriptionTimer()).willReturn(mockTimer);
        given(subscriptionRepository.findByUserIdAndStatusInOrderByCreatedAtDesc(eq(100L), anyList()))
            .willReturn(Collections.emptyList());
        given(subscriptionPlanRepository.findById(1L)).willReturn(Optional.of(testPlan));
        given(subscriptionRepository.save(any(Subscription.class))).willReturn(trialSubscription);

        // When
        SubscriptionDto result = subscriptionService.createSubscription(testRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(Subscription.SubscriptionStatus.TRIAL);
        
        verify(subscriptionRepository).save(any(Subscription.class));
        verify(paymentEventService).publishSubscriptionCreated(any(Subscription.class));
    }

    @Test
    @DisplayName("구독 생성 실패 - 이미 활성 구독 존재")
    void createSubscription_ExistingActiveSubscription_ThrowsException() {
        // Given
        given(subscriptionRepository.findByUserIdAndStatusInOrderByCreatedAtDesc(eq(100L), anyList()))
            .willReturn(List.of(testSubscription));

        // When & Then
        assertThatThrownBy(() -> subscriptionService.createSubscription(testRequest))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("User already has an active subscription");
    }

    @Test
    @DisplayName("구독 생성 실패 - 플랜을 찾을 수 없음")
    void createSubscription_PlanNotFound_ThrowsException() {
        // Given
        given(subscriptionRepository.findByUserIdAndStatusInOrderByCreatedAtDesc(eq(100L), anyList()))
            .willReturn(Collections.emptyList());
        given(subscriptionPlanRepository.findById(1L)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> subscriptionService.createSubscription(testRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Subscription plan not found");
    }

    @Test
    @DisplayName("구독 생성 실패 - 비활성 플랜")
    void createSubscription_InactivePlan_ThrowsException() {
        // Given
        SubscriptionPlan inactivePlan = testPlan.toBuilder().isActive(false).build();
        
        given(subscriptionRepository.findByUserIdAndStatusInOrderByCreatedAtDesc(eq(100L), anyList()))
            .willReturn(Collections.emptyList());
        given(subscriptionPlanRepository.findById(1L)).willReturn(Optional.of(inactivePlan));

        // When & Then
        assertThatThrownBy(() -> subscriptionService.createSubscription(testRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Subscription plan is not active");
    }

    @Test
    @DisplayName("구독 활성화 - 성공")
    void activateSubscription_Success() {
        // Given
        Subscription incompleteSubscription = testSubscription.toBuilder()
            .status(Subscription.SubscriptionStatus.INCOMPLETE)
            .build();
            
        given(subscriptionRepository.findById(1L)).willReturn(Optional.of(incompleteSubscription));
        given(subscriptionRepository.save(any(Subscription.class))).willReturn(testSubscription);

        // When
        SubscriptionDto result = subscriptionService.activateSubscription(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(Subscription.SubscriptionStatus.ACTIVE);
        
        verify(subscriptionRepository).save(any(Subscription.class));
        verify(paymentEventService).publishSubscriptionCreated(any(Subscription.class));
    }

    @Test
    @DisplayName("구독 활성화 실패 - 구독을 찾을 수 없음")
    void activateSubscription_NotFound_ThrowsException() {
        // Given
        given(subscriptionRepository.findById(1L)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> subscriptionService.activateSubscription(1L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Subscription not found");
    }

    @Test
    @DisplayName("사용자 활성 구독 조회 - 존재함")
    void getUserActiveSubscription_Found() {
        // Given
        given(subscriptionRepository.findByUserIdAndStatusInOrderByCreatedAtDesc(eq(100L), anyList()))
            .willReturn(List.of(testSubscription));

        // When
        SubscriptionDto result = subscriptionService.getUserActiveSubscription(100L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(100L);
        assertThat(result.getStatus()).isEqualTo(Subscription.SubscriptionStatus.ACTIVE);
    }

    @Test
    @DisplayName("사용자 활성 구독 조회 - 존재하지 않음")
    void getUserActiveSubscription_NotFound() {
        // Given
        given(subscriptionRepository.findByUserIdAndStatusInOrderByCreatedAtDesc(eq(100L), anyList()))
            .willReturn(Collections.emptyList());

        // When
        SubscriptionDto result = subscriptionService.getUserActiveSubscription(100L);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("사용자 전체 구독 조회")
    void getUserSubscriptions_Success() {
        // Given
        Subscription canceledSubscription = testSubscription.toBuilder()
            .id(2L)
            .status(Subscription.SubscriptionStatus.CANCELED)
            .build();
        
        given(subscriptionRepository.findByUserIdOrderByCreatedAtDesc(100L))
            .willReturn(List.of(testSubscription, canceledSubscription));

        // When
        List<SubscriptionDto> result = subscriptionService.getUserSubscriptions(100L);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getStatus()).isEqualTo(Subscription.SubscriptionStatus.ACTIVE);
        assertThat(result.get(1).getStatus()).isEqualTo(Subscription.SubscriptionStatus.CANCELED);
    }

    @Test
    @DisplayName("구독 취소 - 즉시 취소")
    void cancelSubscription_ImmediateCancel_Success() {
        // Given
        given(subscriptionRepository.findById(1L)).willReturn(Optional.of(testSubscription));
        
        Subscription canceledSubscription = testSubscription.toBuilder()
            .status(Subscription.SubscriptionStatus.CANCELED)
            .canceledAt(LocalDateTime.now())
            .build();
            
        given(subscriptionRepository.save(any(Subscription.class))).willReturn(canceledSubscription);

        // When
        SubscriptionDto result = subscriptionService.cancelSubscription(1L, false);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(Subscription.SubscriptionStatus.CANCELED);
        
        verify(subscriptionRepository).save(any(Subscription.class));
    }

    @Test
    @DisplayName("구독 취소 - 기간 만료 시 취소")
    void cancelSubscription_CancelAtPeriodEnd_Success() {
        // Given
        given(subscriptionRepository.findById(1L)).willReturn(Optional.of(testSubscription));
        
        Subscription scheduledCancelSubscription = testSubscription.toBuilder()
            .cancelAtPeriodEnd(true)
            .build();
            
        given(subscriptionRepository.save(any(Subscription.class))).willReturn(scheduledCancelSubscription);

        // When
        SubscriptionDto result = subscriptionService.cancelSubscription(1L, true);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCancelAtPeriodEnd()).isTrue();
        assertThat(result.getStatus()).isEqualTo(Subscription.SubscriptionStatus.ACTIVE);
        
        verify(subscriptionRepository).save(any(Subscription.class));
        verify(paymentEventService, never()).publishSubscriptionCanceled(any(Subscription.class));
    }

    @Test
    @DisplayName("구독 취소 실패 - 구독을 찾을 수 없음")
    void cancelSubscription_NotFound_ThrowsException() {
        // Given
        given(subscriptionRepository.findById(1L)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> subscriptionService.cancelSubscription(1L, false))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Subscription not found");
    }

    @Test
    @DisplayName("체크아웃 세션 생성 - 미구현")
    void createCheckoutSession_NotImplemented() {
        // When & Then
        assertThatThrownBy(() -> subscriptionService.createCheckoutSession(testRequest))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessage("TossPayments checkout session not implemented yet");
    }
}
