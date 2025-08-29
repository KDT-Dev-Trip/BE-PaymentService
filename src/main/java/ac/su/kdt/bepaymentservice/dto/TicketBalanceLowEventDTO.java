package ac.su.kdt.bepaymentservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

// 티켓 잔액 부족 이벤트
// 티켓 잔액 부족 이벤트는 사용자가 티켓을 사용할 때 잔액이 부족할 때 발생하는 이벤트입니다.
// 이벤트는 잔액, 임계값, 예상 소진 날짜, 권장 충전량 등의 정보를 포함합니다.

public record TicketBalanceLowEventDTO(
    @JsonProperty("event_type")
    String eventType,
    
    @JsonProperty("event_id")
    String eventId,
    
    @JsonProperty("user_id")
    Long userId,
    
    @JsonProperty("auth_user_id")
    String authUserId,
    
    String email,
    Integer currentBalance,
    Integer thresholdLimit, // 임계값
    String subscriptionPlan,
    LocalDateTime lastUsedAt, // 마지막 티켓 사용 시간
    LocalDateTime predictedDepletionDate, // 예상 소진 날짜
    Boolean isAutoRechargeEnabled, // 자동 충전 활성화 여부
    Integer suggestedRechargeAmount, // 권장 충전량
    
    @JsonProperty("timestamp")
    long timestamp
) {
    public static TicketBalanceLowEventDTO createDefault(
            Long userId,
            String authUserId,
            String email,
            Integer currentBalance,
            Integer thresholdLimit,
            String subscriptionPlan,
            LocalDateTime lastUsedAt,
            Boolean isAutoRechargeEnabled,
            Integer suggestedRechargeAmount
    ) {
        return new TicketBalanceLowEventDTO(
            "payment.ticket-balance-low",
            java.util.UUID.randomUUID().toString(),
            userId,
            authUserId,
            email,
            currentBalance,
            thresholdLimit,
            subscriptionPlan,
            lastUsedAt,
            calculatePredictedDepletionDate(currentBalance, lastUsedAt),
            isAutoRechargeEnabled,
            suggestedRechargeAmount,
            System.currentTimeMillis()
        );
    }
    
    private static LocalDateTime calculatePredictedDepletionDate(Integer currentBalance, LocalDateTime lastUsedAt) {
        if (currentBalance <= 0) {
            return LocalDateTime.now();
        }
        // 간단한 예측: 현재 잔액 기준으로 하루에 1티켓씩 사용한다고 가정
        return LocalDateTime.now().plusDays(currentBalance);
    }
}