package ac.su.kdt.bepaymentservice.controller;

import ac.su.kdt.bepaymentservice.dto.CreateSubscriptionRequest;
import ac.su.kdt.bepaymentservice.dto.SubscriptionDto;
import ac.su.kdt.bepaymentservice.service.SubscriptionService;
import ac.su.kdt.bepaymentservice.util.GatewayAuthUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
@Slf4j
public class SubscriptionController {
    
    private final SubscriptionService subscriptionService;
    
    @PostMapping
    public ResponseEntity<SubscriptionDto> createSubscription(
            @RequestParam String userId,
            @Valid @RequestBody CreateSubscriptionRequest request) {
        try {
            // String userId를 Long으로 변환하여 request에 설정
            Long userIdLong = convertUserIdToLong(userId);
            request.setUserId(userIdLong);
            
            SubscriptionDto subscription = subscriptionService.createSubscription(request);
            
            log.info("💳 Real API: Subscription created successfully for user: {}, plan: {}", 
                    userId, subscription.getPlan().getPlanName());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(subscription);
        } catch (IllegalStateException | IllegalArgumentException e) {
            log.error("Error creating subscription: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Unexpected error creating subscription", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping("/checkout")
    public ResponseEntity<Map<String, String>> createCheckoutSession(
            @RequestParam String userId,
            @Valid @RequestBody CreateSubscriptionRequest request) {
        try {
            // String userId를 Long으로 변환하여 request에 설정
            Long userIdLong = convertUserIdToLong(userId);
            request.setUserId(userIdLong);
            
            String checkoutUrl = subscriptionService.createCheckoutSession(request);
            
            log.info("💳 Real API: Checkout session created for user: {}, planId: {}", 
                    userId, request.getPlanId());
            
            return ResponseEntity.ok(Map.of(
                "checkoutUrl", checkoutUrl,
                "apiType", "REAL_BUSINESS_API",
                "operation", "CREATE_CHECKOUT_SESSION"
            ));
        } catch (UnsupportedOperationException e) {
            log.error("TossPayments checkout not implemented: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
        } catch (IllegalArgumentException e) {
            log.error("Error creating checkout session: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Unexpected error creating checkout session", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/users/{userId}")
    public ResponseEntity<List<SubscriptionDto>> getUserSubscriptions(@PathVariable String userId) {
        try {
            // Gateway 인증 확인
            if (!GatewayAuthUtils.isAuthenticated()) {
                log.warn("Unauthorized access attempt to user subscriptions for user: {}", userId);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            // 본인의 구독 정보만 조회 가능
            if (!GatewayAuthUtils.isCurrentUser(userId)) {
                log.warn("User {} attempted to access subscriptions of user {}", 
                        GatewayAuthUtils.getCurrentUserId(), userId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            log.info("Fetching subscriptions for authenticated user: {} ({})", 
                    GatewayAuthUtils.getCurrentUserInfo(), userId);
            
            // String userId를 Long으로 변환하여 서비스 호출
            Long userIdLong = convertUserIdToLong(userId);
            List<SubscriptionDto> subscriptions = subscriptionService.getUserSubscriptions(userIdLong);
            
            log.info("💳 Real API: Retrieved {} subscriptions for user: {}", 
                    subscriptions.size(), userId);
            
            return ResponseEntity.ok(subscriptions);
        } catch (Exception e) {
            log.error("Error fetching subscriptions for user: {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/{subscriptionId}")
    public ResponseEntity<SubscriptionDto> getSubscription(@PathVariable Long subscriptionId) {
        try {
            // getUserActiveSubscription 메소드를 사용하거나 별도 구현 필요
            // 현재 SubscriptionService에 getSubscription 메소드가 없으므로 제거
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
        } catch (Exception e) {
            log.error("Error fetching subscription: {}", subscriptionId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PutMapping("/{subscriptionId}/cancel")
    public ResponseEntity<SubscriptionDto> cancelSubscription(@PathVariable Long subscriptionId) {
        try {
            // cancelSubscription은 subscriptionId와 cancelAtPeriodEnd 파라미터 필요
            SubscriptionDto subscription = subscriptionService.cancelSubscription(subscriptionId, false);
            
            log.info("💳 Real API: Subscription cancelled successfully, ID: {}, status: {}", 
                    subscriptionId, subscription.getStatus());
            
            return ResponseEntity.ok(subscription);
        } catch (IllegalArgumentException e) {
            log.error("Subscription not found: {}", subscriptionId);
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            log.error("Error cancelling subscription: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Unexpected error cancelling subscription: {}", subscriptionId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 사용자 ID를 UUID String에서 Long으로 변환
     * UUID의 hash 값을 Long으로 사용하여 기존 서비스와 호환성 유지
     * 실제 API 호출에서 구독 이벤트들이 자동 발행됨
     */
    private Long convertUserIdToLong(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        
        // UUID 문자열의 해시코드를 Long으로 변환
        // 음수를 양수로 변환하기 위해 Math.abs 사용
        long hash = Math.abs((long) userId.hashCode());
        
        // Long 범위를 벗어나지 않도록 보정
        if (hash < 0) {
            hash = Math.abs(hash);
        }
        
        log.debug("Converted user ID {} to Long: {}", userId, hash);
        return hash;
    }
}