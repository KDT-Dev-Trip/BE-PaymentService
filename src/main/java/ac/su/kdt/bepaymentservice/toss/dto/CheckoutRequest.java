package ac.su.kdt.bepaymentservice.toss.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutRequest {
    private Long amount;
    private String orderId;
    private String orderName;
    private String customerEmail;
    private String customerName;
    private String successUrl;
    private String failUrl;
}