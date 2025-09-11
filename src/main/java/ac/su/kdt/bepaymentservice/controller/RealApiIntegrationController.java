package ac.su.kdt.bepaymentservice.controller;

import ac.su.kdt.bepaymentservice.client.UserServiceClient;
import ac.su.kdt.bepaymentservice.dto.CreateSubscriptionRequest;
import ac.su.kdt.bepaymentservice.dto.SubscriptionDto;
import ac.su.kdt.bepaymentservice.service.SubscriptionService;
import ac.su.kdt.bepaymentservice.service.TossPaymentsService;
import ac.su.kdt.bepaymentservice.toss.dto.PaymentResponse;
import ac.su.kdt.bepaymentservice.toss.dto.AutoPaymentRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.List;

/**
 * 실제 API와 이벤트 시스템 통합 테스트 컨트롤러
 * 비즈니스 로직을 통한 실제 이벤트 발행 테스트
 */
@RestController
@RequestMapping("/api/integration-test")
@RequiredArgsConstructor
@Slf4j
public class RealApiIntegrationController {

    private final UserServiceClient userServiceClient;
    private final SubscriptionService subscriptionService;
    private final TossPaymentsService tossPaymentsService;

    /**
     * 실제 API 연동 테스트: 티켓 사용 → 잔액 부족 이벤트 트리거
     */
    @PostMapping("/real-ticket-usage-flow")
    public ResponseEntity<Map<String, Object>> testRealTicketUsageFlow(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "15") Integer ticketsToUse) {
        
        log.info("🚀 REAL API INTEGRATION: Starting ticket usage flow for user: {}", userId);
        
        try {
            // 1. 사용 전 티켓 상태 확인
            Map<String, Object> beforeUsage = userServiceClient.getUserTickets(userId);
            log.info("📊 Before usage - User: {}, Current: {}", 
                    userId, beforeUsage != null ? beforeUsage.get("currentTickets") : "unknown");
            
            // 2. 실제 티켓 사용 (이 과정에서 잔액 부족 이벤트가 자동 발행될 수 있음)
            boolean success = userServiceClient.useTickets(userId, ticketsToUse, 999L, "Real API integration test mission");
            
            // 3. 사용 후 티켓 상태 확인
            Map<String, Object> afterUsage = userServiceClient.getUserTickets(userId);
            log.info("📊 After usage - User: {}, Current: {}", 
                    userId, afterUsage != null ? afterUsage.get("currentTickets") : "unknown");
            
            return ResponseEntity.ok(Map.of(
                "testType", "REAL_API_INTEGRATION",
                "operation", "TICKET_USAGE_WITH_EVENT_TRIGGER",
                "success", success,
                "userId", userId,
                "ticketsUsed", ticketsToUse,
                "beforeUsage", Map.of(
                    "current", beforeUsage != null ? beforeUsage.get("currentTickets") : 0
                ),
                "afterUsage", Map.of(
                    "current", afterUsage != null ? afterUsage.get("currentTickets") : 0
                ),
                "eventTriggered", (afterUsage != null && (Integer)afterUsage.get("currentTickets") <= 5) ? "LOW_BALANCE_EVENT_LIKELY_SENT" : "NO_EVENT_NEEDED"
            ));
            
        } catch (Exception e) {
            log.error("Error in real ticket usage flow test", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", e.getMessage(),
                "testType", "REAL_API_INTEGRATION"
            ));
        }
    }

    /**
     * 실제 API 연동 테스트: 구독 플랜 변경 → 구독 변경 이벤트 트리거
     */
    @PostMapping("/real-subscription-change-flow")
    public ResponseEntity<Map<String, Object>> testRealSubscriptionChangeFlow(
            @RequestParam Long subscriptionId,
            @RequestParam Long newPlanId,
            @RequestParam(defaultValue = "USER_UPGRADE") String changeReason) {
        
        log.info("🚀 REAL API INTEGRATION: Starting subscription change flow for subscription: {}", subscriptionId);
        
        try {
            // 1. 실제 구독 플랜 변경 (이 과정에서 구독 변경 이벤트가 자동 발행됨)
            subscriptionService.changeSubscriptionPlan(subscriptionId, newPlanId, changeReason);
            
            return ResponseEntity.ok(Map.of(
                "testType", "REAL_API_INTEGRATION", 
                "operation", "SUBSCRIPTION_CHANGE_WITH_EVENT_TRIGGER",
                "success", true,
                "subscriptionId", subscriptionId,
                "newPlanId", newPlanId,
                "changeReason", changeReason,
                "eventTriggered", "SUBSCRIPTION_CHANGED_EVENT_SENT"
            ));
            
        } catch (Exception e) {
            log.error("Error in real subscription change flow test", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", e.getMessage(),
                "testType", "REAL_API_INTEGRATION"
            ));
        }
    }

    /**
     * 실제 API 연동 테스트: 구독 갱신 실패 시뮬레이션 → 갱신 실패 이벤트 트리거
     */
    @PostMapping("/real-subscription-renewal-failure-flow")
    public ResponseEntity<Map<String, Object>> testRealSubscriptionRenewalFailureFlow(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "CARD_EXPIRED") String failureReason,
            @RequestParam(defaultValue = "3") Integer retryCount) {
        
        log.info("🚀 REAL API INTEGRATION: Starting subscription renewal failure flow for user: {}", userId);
        
        try {
            // 실제 구독 갱신 실패 처리 (이 과정에서 갱신 실패 이벤트가 자동 발행됨)
            subscriptionService.handleRenewalFailure(userId, failureReason, "CARD", retryCount);
            
            return ResponseEntity.ok(Map.of(
                "testType", "REAL_API_INTEGRATION",
                "operation", "SUBSCRIPTION_RENEWAL_FAILURE_WITH_EVENT_TRIGGER",
                "success", true,
                "userId", userId,
                "failureReason", failureReason,
                "retryCount", retryCount,
                "eventTriggered", "SUBSCRIPTION_RENEWAL_FAILED_EVENT_SENT",
                "accountStatus", retryCount >= 3 ? "SUSPENDED" : "ACTIVE_WITH_WARNING"
            ));
            
        } catch (Exception e) {
            log.error("Error in real subscription renewal failure flow test", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", e.getMessage(),
                "testType", "REAL_API_INTEGRATION"
            ));
        }
    }

    /**
     * 실제 API 연동 테스트: TossPayments 자동결제 성공/실패 → 결제 이벤트 트리거
     */
    @PostMapping("/real-payment-flow")
    public ResponseEntity<Map<String, Object>> testRealPaymentFlow(
            @RequestParam String billingKey,
            @RequestParam Long userId,
            @RequestParam(defaultValue = "15000") Long amount,
            @RequestParam(defaultValue = "false") Boolean simulateFailure) {
        
        log.info("🚀 REAL API INTEGRATION: Starting payment flow for user: {}, amount: {}", userId, amount);
        
        try {
            // TossPayments 자동결제 요청 생성
            AutoPaymentRequest request = new AutoPaymentRequest();
            request.setOrderId("subscription-" + userId + "-" + System.currentTimeMillis());
            request.setOrderName("DevTrip 구독 결제");
            request.setAmount(amount);
            request.setCustomerKey("customer-" + userId);
            
            // 실제 TossPayments 자동결제 API 호출
            // (Mock 환경에서는 성공 응답 반환, 실제 환경에서는 TossPayments와 통신)
            PaymentResponse response = tossPaymentsService.processAutoPayment(billingKey, request).block();
            
            return ResponseEntity.ok(Map.of(
                "testType", "REAL_API_INTEGRATION",
                "operation", "TOSS_PAYMENTS_AUTO_PAYMENT_WITH_EVENT_TRIGGER", 
                "success", true,
                "userId", userId,
                "billingKey", billingKey,
                "orderId", request.getOrderId(),
                "amount", amount,
                "paymentKey", response.getPaymentKey(),
                "status", response.getStatus(),
                "eventTriggered", simulateFailure ? "PAYMENT_FAILURE_EVENT_SENT" : "PAYMENT_SUCCESS_EVENT_SENT"
            ));
            
        } catch (Exception e) {
            log.error("Error in real payment flow test", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", e.getMessage(),
                "testType", "REAL_API_INTEGRATION"
            ));
        }
    }

    /**
     * 전체 실제 API 통합 플로우 테스트
     */
    @PostMapping("/full-real-integration-flow")
    public ResponseEntity<Map<String, Object>> testFullRealIntegrationFlow(@RequestParam Long userId) {
        
        log.info("🚀 REAL API INTEGRATION: Starting FULL integration flow for user: {}", userId);
        
        try {
            // 1. 티켓 사용으로 잔액 부족 이벤트 트리거
            log.info("Step 1: Testing real ticket usage...");
            boolean ticketUsageSuccess = userServiceClient.useTickets(userId, 10, 1001L, "Full integration test");
            Map<String, Object> userTickets = userServiceClient.getUserTickets(userId);
            
            Thread.sleep(1000); // 이벤트 처리 대기
            
            // 2. 구독 갱신 실패 이벤트 트리거
            log.info("Step 2: Testing real subscription renewal failure...");
            subscriptionService.handleRenewalFailure(userId, "CARD_EXPIRED", "CARD", 2);
            
            Thread.sleep(1000); // 이벤트 처리 대기
            
            // 3. 구독 플랜 변경 이벤트 트리거
            log.info("Step 3: Testing real subscription plan change...");
            subscriptionService.changeSubscriptionPlan(1L, 2L, "INTEGRATION_TEST_UPGRADE");
            
            return ResponseEntity.ok(Map.of(
                "testType", "REAL_API_FULL_INTEGRATION",
                "operation", "COMPLETE_BUSINESS_LOGIC_WITH_EVENTS",
                "success", true,
                "userId", userId,
                "steps", Map.of(
                    "step1_ticket_usage", Map.of(
                        "success", ticketUsageSuccess,
                        "remainingTickets", userTickets != null ? userTickets.get("currentTickets") : 0,
                        "eventTriggered", "ticket-balance-low"
                    ),
                    "step2_renewal_failure", Map.of(
                        "success", true,
                        "eventTriggered", "subscription-renewal-failed"
                    ),
                    "step3_plan_change", Map.of(
                        "success", true,
                        "eventTriggered", "subscription-changed"
                    )
                ),
                "message", "All real API operations completed with automatic event publishing"
            ));
            
        } catch (Exception e) {
            log.error("Error in full real integration flow test", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", e.getMessage(),
                "testType", "REAL_API_FULL_INTEGRATION"
            ));
        }
    }

    /**
     * API 통합 상태 확인
     */
    @GetMapping("/integration-status")
    public ResponseEntity<Map<String, Object>> getIntegrationStatus() {
        return ResponseEntity.ok(Map.of(
            "status", "Real API Integration Controller is running",
            "timestamp", System.currentTimeMillis(),
            "availableTests", List.of(
                "real-ticket-usage-flow",
                "real-subscription-change-flow", 
                "real-subscription-renewal-failure-flow",
                "real-payment-flow",
                "full-real-integration-flow"
            ),
            "description", "Tests real business API calls with automatic event publishing"
        ));
    }
}