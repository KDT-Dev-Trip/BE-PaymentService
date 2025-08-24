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
    public ResponseEntity<SubscriptionDto> createSubscription(@Valid @RequestBody CreateSubscriptionRequest request) {
        try {
            SubscriptionDto subscription = subscriptionService.createSubscription(request);
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
    public ResponseEntity<Map<String, String>> createCheckoutSession(@Valid @RequestBody CreateSubscriptionRequest request) {
        try {
            String checkoutUrl = subscriptionService.createCheckoutSession(request);
            return ResponseEntity.ok(Map.of("checkoutUrl", checkoutUrl));
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
            return ResponseEntity.ok(subscriptions);
        } catch (Exception e) {
            log.error("Error fetching user subscriptions for user: {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/users/{userId}/active")
    public ResponseEntity<SubscriptionDto> getUserActiveSubscription(@PathVariable String userId) {
        try {
            // Gateway 인증 확인
            if (!GatewayAuthUtils.isAuthenticated()) {
                log.warn("Unauthorized access attempt to active subscription for user: {}", userId);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            // 본인의 구독 정보만 조회 가능
            if (!GatewayAuthUtils.isCurrentUser(userId)) {
                log.warn("User {} attempted to access active subscription of user {}", 
                        GatewayAuthUtils.getCurrentUserId(), userId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            log.info("Fetching active subscription for authenticated user: {} ({})", 
                    GatewayAuthUtils.getCurrentUserInfo(), userId);
            
            // String userId를 Long으로 변환하여 서비스 호출
            Long userIdLong = convertUserIdToLong(userId);
            SubscriptionDto subscription = subscriptionService.getUserActiveSubscription(userIdLong);
            if (subscription != null) {
                return ResponseEntity.ok(subscription);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error fetching active subscription for user: {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping("/{subscriptionId}/cancel")
    public ResponseEntity<SubscriptionDto> cancelSubscription(
            @PathVariable Long subscriptionId,
            @RequestParam(defaultValue = "true") boolean cancelAtPeriodEnd) {
        try {
            SubscriptionDto subscription = subscriptionService.cancelSubscription(subscriptionId, cancelAtPeriodEnd);
            return ResponseEntity.ok(subscription);
        } catch (IllegalArgumentException e) {
            log.error("Error cancelling subscription: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Unexpected error cancelling subscription: {}", subscriptionId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/{subscriptionId}")
    public ResponseEntity<SubscriptionDto> getSubscription(@PathVariable Long subscriptionId) {
        // This would typically be implemented to fetch a specific subscription
        // For now, we'll return not implemented
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
    
    /**
     * 사용자 ID를 UUID String에서 Long으로 변환
     * UUID의 hash 값을 Long으로 사용하여 기존 서비스와 호환성 유지
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