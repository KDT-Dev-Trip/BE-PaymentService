package ac.su.kdt.bepaymentservice.toss.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmRequest {
    private String paymentKey;
    private String orderId;
    private Long amount;
}