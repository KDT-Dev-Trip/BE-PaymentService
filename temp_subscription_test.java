package ac.su.kdt.bepaymentservice.controller;

import ac.su.kdt.bepaymentservice.dto.CreateSubscriptionRequest;
import ac.su.kdt.bepaymentservice.dto.SubscriptionDto;
import ac.su.kdt.bepaymentservice.dto.SubscriptionPlanDto;
import ac.su.kdt.bepaymentservice.entity.Subscription;
import ac.su.kdt.bepaymentservice.entity.SubscriptionPlan;
import ac.su.kdt.bepaymentservice.service.SubscriptionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SubscriptionController.class)
@DisplayName("SubscriptionController 통합 테스트")
class SubscriptionControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private SubscriptionService subscriptionService;
    
    private SubscriptionDto testSubscriptionDto;
    private CreateSubscriptionRequest createRequest;
    
    @BeforeEach
    void setUp() {
        SubscriptionPlanDto planDto = SubscriptionPlanDto.builder()
                .id(1L)
                .planName("Economy Class")
                .planType(SubscriptionPlan.PlanType.ECONOMY_CLASS)
                .monthlyPrice(new BigDecimal("29.00"))
                .yearlyPrice(new BigDecimal("290.00"))
                .isActive(true)
                .build();
        
        testSubscriptionDto = SubscriptionDto.builder()
                .id(1L)
                .userId(1L)
                .plan(planDto)
                .status(Subscription.SubscriptionStatus.ACTIVE)
                .billingCycle(Subscription.BillingCycle.MONTHLY)
                .amount(new BigDecimal("29.00"))
                .currency("KRW")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        createRequest = CreateSubscriptionRequest.builder()
                .userId(1L)
                .planId(1L)
                .billingCycle(Subscription.BillingCycle.MONTHLY)
                .build();
    }
    
    @Test
    @DisplayName("구독 생성 API가 정상적으로 동작한다")
    void createSubscription_Success() throws Exception {
        // Given
        given(subscriptionService.createSubscription(any(CreateSubscriptionRequest.class)))
                .willReturn(testSubscriptionDto);
        
        // When & Then
        mockMvc.perform(post("/api/v1/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.userId").value(1L))
                .andExpect(jsonPath("$.plan.id").value(1L))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.billingCycle").value("MONTHLY"))
                .andExpect(jsonPath("$.amount").value(29.00))
                .andExpect(jsonPath("$.currency").value("KRW"));
        
        verify(subscriptionService).createSubscription(any(CreateSubscriptionRequest.class));
    }
    
    @Test
    @DisplayName("잘못된 요청으로 구독 생성 시 400 에러를 반환한다")
    void createSubscription_InvalidRequest_Returns400() throws Exception {
        // Given
        CreateSubscriptionRequest invalidRequest = CreateSubscriptionRequest.builder()
                .userId(null) // Required field missing
                .planId(1L)
                .billingCycle(Subscription.BillingCycle.MONTHLY)
                .build();
        
        // When & Then
        mockMvc.perform(post("/api/v1/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
        
        verify(subscriptionService, never()).createSubscription(any(CreateSubscriptionRequest.class));
    }
    
    @Test
    @DisplayName("이미 활성 구독이 있는 경우 400 에러를 반환한다")
    void createSubscription_AlreadyHasActiveSubscription_Returns400() throws Exception {
        // Given
        given(subscriptionService.createSubscription(any(CreateSubscriptionRequest.class)))
                .willThrow(new IllegalStateException("User already has an active subscription"));
        
        // When & Then
        mockMvc.perform(post("/api/v1/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest());
        
        verify(subscriptionService).createSubscription(any(CreateSubscriptionRequest.class));
    }
    
    @Test
    @DisplayName("체크아웃 세션 생성 API가 정상적으로 동작한다")
    void createCheckoutSession_Success() throws Exception {
        // Given
        String checkoutUrl = "https://checkout.stripe.com/pay/cs_test123";
        createRequest.setSuccessUrl("https://example.com/success");
        createRequest.setCancelUrl("https://example.com/cancel");
        
        given(subscriptionService.createCheckoutSession(any(CreateSubscriptionRequest.class)))
                .willReturn(checkoutUrl);
        
        // When & Then
        mockMvc.perform(post("/api/v1/subscriptions/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkoutUrl").value(checkoutUrl));
        
        verify(subscriptionService).createCheckoutSession(any(CreateSubscriptionRequest.class));
    }
    
    @Test
    @DisplayName("TossPayments 미구현 기능 호출 시 501 에러를 반환한다")
    void createCheckoutSession_StripeException_Returns500() throws Exception {
        // Given
        createRequest.setSuccessUrl("https://example.com/success");
        createRequest.setCancelUrl("https://example.com/cancel");
        
        given(subscriptionService.createCheckoutSession(any(CreateSubscriptionRequest.class)))
                .willThrow(new UnsupportedOperationException("TossPayments checkout not implemented yet"));
        
        // When & Then
        mockMvc.perform(post("/api/v1/subscriptions/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isNotImplemented());
        
        verify(subscriptionService).createCheckoutSession(any(CreateSubscriptionRequest.class));
    }
    
    @Test
    @DisplayName("사용자 구독 목록 조회 API가 정상적으로 동작한다")
    void getUserSubscriptions_Success() throws Exception {
        // Given
        List<SubscriptionDto> subscriptions = List.of(testSubscriptionDto);
        given(subscriptionService.getUserSubscriptions(1L))
                .willReturn(subscriptions);
        
        // When & Then
        mockMvc.perform(get("/api/v1/subscriptions/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].userId").value(1L))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
        
        verify(subscriptionService).getUserSubscriptions(1L);
    }
    
    @Test
    @DisplayName("사용자 활성 구독 조회 API가 정상적으로 동작한다")
    void getUserActiveSubscription_Success() throws Exception {
        // Given
        given(subscriptionService.getUserActiveSubscription(1L))
                .willReturn(testSubscriptionDto);
        
        // When & Then
        mockMvc.perform(get("/api/v1/subscriptions/users/1/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.userId").value(1L))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        
        verify(subscriptionService).getUserActiveSubscription(1L);
    }
    
    @Test
    @DisplayName("활성 구독이 없는 경우 404를 반환한다")
    void getUserActiveSubscription_NoActiveSubscription_Returns404() throws Exception {
        // Given
        given(subscriptionService.getUserActiveSubscription(1L))
                .willReturn(null);
        
        // When & Then
        mockMvc.perform(get("/api/v1/subscriptions/users/1/active"))
                .andExpect(status().isNotFound());
        
        verify(subscriptionService).getUserActiveSubscription(1L);
    }
    
    @Test
    @DisplayName("구독 취소 API가 정상적으로 동작한다")
    void cancelSubscription_Success() throws Exception {
        // Given
        SubscriptionDto cancelledSubscription = SubscriptionDto.builder()
                .id(1L)
                .userId(1L)
                .status(Subscription.SubscriptionStatus.CANCELED)
                .cancelAtPeriodEnd(false)
                .canceledAt(LocalDateTime.now())
                .build();
        
        given(subscriptionService.cancelSubscription(1L, false))
                .willReturn(cancelledSubscription);
        
        // When & Then
        mockMvc.perform(post("/api/v1/subscriptions/1/cancel")
                        .param("cancelAtPeriodEnd", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.status").value("CANCELED"))
                .andExpect(jsonPath("$.cancelAtPeriodEnd").value(false))
                .andExpect(jsonPath("$.canceledAt").exists());
        
        verify(subscriptionService).cancelSubscription(1L, false);
    }
    
    @Test
    @DisplayName("기간 종료 시 취소 설정이 정상적으로 동작한다")
    void cancelSubscription_CancelAtPeriodEnd_Success() throws Exception {
        // Given
        SubscriptionDto scheduledForCancellation = SubscriptionDto.builder()
                .id(1L)
                .userId(1L)
                .status(Subscription.SubscriptionStatus.ACTIVE)
                .cancelAtPeriodEnd(true)
                .build();
        
        given(subscriptionService.cancelSubscription(1L, true))
                .willReturn(scheduledForCancellation);
        
        // When & Then
        mockMvc.perform(post("/api/v1/subscriptions/1/cancel")
                        .param("cancelAtPeriodEnd", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.cancelAtPeriodEnd").value(true));
        
        verify(subscriptionService).cancelSubscription(1L, true);
    }
    
    @Test
    @DisplayName("존재하지 않는 구독 취소 시 400 에러를 반환한다")
    void cancelSubscription_NotFound_Returns400() throws Exception {
        // Given
        given(subscriptionService.cancelSubscription(1L, true))
                .willThrow(new IllegalArgumentException("Subscription not found"));
        
        // When & Then
        mockMvc.perform(post("/api/v1/subscriptions/1/cancel"))
                .andExpect(status().isBadRequest());
        
        verify(subscriptionService).cancelSubscription(1L, true);
    }
}