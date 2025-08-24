package ac.su.kdt.bepaymentservice.toss.dto;

import lombok.Data;

@Data
public class AutoPaymentRequest {
    private Long amount;
    private String customerKey;
    private String orderId;
    private String orderName;
    private String customerEmail;
    private String customerName;
    private Long taxFreeAmount = 0L;
    private Long taxExemptionAmount = 0L;
}