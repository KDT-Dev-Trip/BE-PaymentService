package ac.su.kdt.bepaymentservice.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

// 결제 이벤트
// 결제 이벤트는 결제 시스템에서 발생하는 이벤트입니다.
// 이벤트는 이벤트 ID, 이벤트 유형, 사용자 ID, 팀 ID, 타임스탬프, 데이터 등의 정보를 포함합니다.

public class PaymentEvent {
    private String eventId;
    private String eventType;
    private LocalDateTime timestamp;
    private Long userId;
    private Long teamId;
    private Map<String, Object> data;
    
    public enum EventType {
        SUBSCRIPTION_CREATED,
        SUBSCRIPTION_ACTIVATED,
        SUBSCRIPTION_CANCELLED,
        SUBSCRIPTION_EXPIRED,
        SUBSCRIPTION_EXPIRING,
        SUBSCRIPTION_STATUS_UPDATED,
        PAYMENT_SUCCEEDED,
        PAYMENT_FAILED,
        PAYMENT_REFUNDED,
        TICKETS_USED,
        TICKETS_REFUNDED,
        TICKETS_REFILLED,
        TICKET_BALANCE_LOW
    }
}