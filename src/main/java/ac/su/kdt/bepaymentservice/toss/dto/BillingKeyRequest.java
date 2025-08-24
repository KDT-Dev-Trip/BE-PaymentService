package ac.su.kdt.bepaymentservice.toss.dto;

import lombok.Data;

@Data
public class BillingKeyRequest {
    private String authKey;
    private String customerKey;
}