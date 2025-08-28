package ac.su.kdt.bepaymentservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record PaymentSubscriptionChangedEventDTO(
    @JsonProperty("event_type")
    String eventType,
    
    @JsonProperty("event_id")
    String eventId,
    
    @JsonProperty("user_id")
    Long userId,
    
    @JsonProperty("auth_user_id")
    String authUserId,
    
    String email,
    String oldPlan,
    String newPlan,
    String changeReason, // "USER_UPGRADE", "USER_DOWNGRADE", "ADMIN_CHANGE", "PAYMENT_FAILURE_DOWNGRADE"
    Double oldPrice,
    Double newPrice,
    String currency,
    LocalDateTime changedAt,
    LocalDateTime effectiveDate,
    Boolean isProratedRefund, // 중간 변경 시 환불 여부
    Double proratedAmount,
    
    @JsonProperty("timestamp")
    long timestamp
) {
    public static PaymentSubscriptionChangedEventDTO createDefault(
            Long userId,
            String authUserId,
            String email,
            String oldPlan,
            String newPlan,
            String changeReason,
            Double oldPrice,
            Double newPrice,
            String currency,
            Boolean isProratedRefund,
            Double proratedAmount
    ) {
        return new PaymentSubscriptionChangedEventDTO(
            "payment.subscription-changed",
            java.util.UUID.randomUUID().toString(),
            userId,
            authUserId,
            email,
            oldPlan,
            newPlan,
            changeReason,
            oldPrice,
            newPrice,
            currency,
            LocalDateTime.now(),
            LocalDateTime.now(), // 즉시 적용
            isProratedRefund,
            proratedAmount,
            System.currentTimeMillis()
        );
    }
}