package ac.su.kdt.bepaymentservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record SubscriptionRenewalFailedEventDTO(
    @JsonProperty("event_type")
    String eventType,
    
    @JsonProperty("event_id")
    String eventId,
    
    @JsonProperty("user_id")
    Long userId,
    
    @JsonProperty("auth_user_id") 
    String authUserId,
    
    String email,
    String subscriptionPlan,
    String failureReason, // "PAYMENT_FAILED", "CARD_EXPIRED", "INSUFFICIENT_FUNDS", etc.
    String paymentMethod, // "CARD", "BANK_TRANSFER", etc.
    LocalDateTime failedAt,
    LocalDateTime nextRetryAt,
    Integer retryAttemptCount,
    Double failedAmount,
    String currency,
    
    @JsonProperty("timestamp")
    long timestamp
) {
    public static SubscriptionRenewalFailedEventDTO createDefault(
            Long userId, 
            String authUserId, 
            String email, 
            String subscriptionPlan, 
            String failureReason,
            String paymentMethod,
            Double failedAmount,
            String currency,
            Integer retryAttemptCount
    ) {
        return new SubscriptionRenewalFailedEventDTO(
            "payment.subscription-renewal-failed",
            java.util.UUID.randomUUID().toString(),
            userId,
            authUserId,
            email,
            subscriptionPlan,
            failureReason,
            paymentMethod,
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(1), // 다음 재시도: 1일 후
            retryAttemptCount,
            failedAmount,
            currency,
            System.currentTimeMillis()
        );
    }
}