package ac.su.kdt.bepaymentservice.controller;

import ac.su.kdt.bepaymentservice.service.PaymentEventPublisher;
import ac.su.kdt.bepaymentservice.service.SubscriptionService;
import ac.su.kdt.bepaymentservice.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 결제 이벤트 테스트용 컨트롤러
 * 개발 환경에서 이벤트 흐름을 테스트하기 위한 용도
 */
@RestController
@RequestMapping("/api/payment/test")
@RequiredArgsConstructor
@Slf4j
public class PaymentEventTestController {

    private final PaymentEventPublisher paymentEventPublisher;
    private final SubscriptionService subscriptionService;
    private final TicketService ticketService;

    /**
     * 구독 갱신 실패 이벤트 테스트
     */
    @PostMapping("/subscription-renewal-failed")
    public ResponseEntity<Map<String, String>> testSubscriptionRenewalFailed(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "CARD_EXPIRED") String failureReason,
            @RequestParam(defaultValue = "1") Integer retryCount) {
        
        log.info("Testing subscription renewal failed event for user: {}", userId);
        
        try {
            // 구독 갱신 실패 이벤트 시뮬레이션
            subscriptionService.handleRenewalFailure(1L, failureReason, "CARD", retryCount);
            
            return ResponseEntity.ok(Map.of(
                "message", "Subscription renewal failed event sent",
                "userId", userId.toString(),
                "failureReason", failureReason,
                "retryCount", retryCount.toString()
            ));
        } catch (Exception e) {
            log.error("Failed to test subscription renewal failed event", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 구독 변경 이벤트 테스트
     */
    @PostMapping("/subscription-changed")
    public ResponseEntity<Map<String, String>> testSubscriptionChanged(
            @RequestParam Long subscriptionId,
            @RequestParam Long newPlanId,
            @RequestParam(defaultValue = "USER_UPGRADE") String changeReason) {
        
        log.info("Testing subscription changed event for subscription: {}", subscriptionId);
        
        try {
            // 구독 변경 이벤트 시뮬레이션
            subscriptionService.changeSubscriptionPlan(subscriptionId, newPlanId, changeReason);
            
            return ResponseEntity.ok(Map.of(
                "message", "Subscription changed event sent",
                "subscriptionId", subscriptionId.toString(),
                "newPlanId", newPlanId.toString(),
                "changeReason", changeReason
            ));
        } catch (Exception e) {
            log.error("Failed to test subscription changed event", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 티켓 잔액 부족 이벤트 테스트 (티켓 사용을 통해)
     */
    @PostMapping("/ticket-balance-low")
    public ResponseEntity<Map<String, String>> testTicketBalanceLow(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "10") Integer ticketsToUse) {
        
        log.info("Testing ticket balance low event for user: {}, using {} tickets", userId, ticketsToUse);
        
        try {
            // 티켓 사용을 통한 잔액 부족 이벤트 시뮬레이션
            boolean success = ticketService.useTickets(userId, ticketsToUse, 123L, "Test mission attempt");
            
            return ResponseEntity.ok(Map.of(
                "message", "Ticket usage attempted (may trigger low balance event)",
                "userId", userId.toString(),
                "ticketsToUse", ticketsToUse.toString(),
                "success", String.valueOf(success)
            ));
        } catch (Exception e) {
            log.error("Failed to test ticket balance low event", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 직접적인 티켓 잔액 부족 이벤트 발행
     */
    @PostMapping("/direct-low-balance-event")
    public ResponseEntity<Map<String, String>> testDirectLowBalanceEvent(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "3") Integer currentBalance,
            @RequestParam(defaultValue = "5") Integer thresholdLimit) {
        
        log.info("Testing direct low balance event for user: {}", userId);
        
        try {
            // 직접 이벤트 발행
            paymentEventPublisher.publishTicketBalanceLow(
                userId,
                userId.toString(),
                "user" + userId + "@example.com",
                currentBalance,
                thresholdLimit,
                "ECONOMY_CLASS",
                java.time.LocalDateTime.now().minusHours(2),
                false,
                10
            );
            
            return ResponseEntity.ok(Map.of(
                "message", "Direct ticket balance low event sent",
                "userId", userId.toString(),
                "currentBalance", currentBalance.toString(),
                "thresholdLimit", thresholdLimit.toString()
            ));
        } catch (Exception e) {
            log.error("Failed to send direct low balance event", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 전체 이벤트 흐름 테스트
     */
    @PostMapping("/full-flow-test")
    public ResponseEntity<Map<String, String>> testFullFlow(@RequestParam Long userId) {
        
        log.info("Testing full payment event flow for user: {}", userId);
        
        try {
            // 1. 티켓 잔액 부족 이벤트
            paymentEventPublisher.publishTicketBalanceLow(
                userId, userId.toString(), "user" + userId + "@example.com",
                2, 5, "ECONOMY_CLASS", java.time.LocalDateTime.now().minusHours(1),
                false, 10
            );
            
            Thread.sleep(500); // 이벤트 처리 대기
            
            // 2. 구독 갱신 실패 이벤트
            paymentEventPublisher.publishRenewalFailed(
                userId, userId.toString(), "user" + userId + "@example.com",
                "ECONOMY_CLASS", "CARD_EXPIRED", "CARD", 15000.0, "KRW", 2
            );
            
            Thread.sleep(500); // 이벤트 처리 대기
            
            // 3. 구독 변경 이벤트
            paymentEventPublisher.publishSubscriptionChanged(
                userId, userId.toString(), "user" + userId + "@example.com",
                "ECONOMY_CLASS", "BUSINESS_CLASS", "USER_UPGRADE",
                15000.0, 25000.0, "KRW", false, null
            );
            
            return ResponseEntity.ok(Map.of(
                "message", "Full payment event flow test completed",
                "userId", userId.toString(),
                "events", "ticket-balance-low, subscription-renewal-failed, subscription-changed"
            ));
            
        } catch (Exception e) {
            log.error("Failed to test full payment event flow", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * 이벤트 상태 확인
     */
    @GetMapping("/event-status")
    public ResponseEntity<Map<String, Object>> getEventStatus() {
        return ResponseEntity.ok(Map.of(
            "status", "Payment Event Test Controller is running",
            "timestamp", System.currentTimeMillis(),
            "availableTests", java.util.List.of(
                "subscription-renewal-failed",
                "subscription-changed", 
                "ticket-balance-low",
                "direct-low-balance-event",
                "full-flow-test"
            )
        ));
    }
}