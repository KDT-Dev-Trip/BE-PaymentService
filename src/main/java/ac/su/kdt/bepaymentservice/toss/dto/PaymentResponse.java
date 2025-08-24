package ac.su.kdt.bepaymentservice.toss.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PaymentResponse {
    @JsonProperty("mId")
    private String mId;
    
    @JsonProperty("lastTransactionKey")
    private String lastTransactionKey;
    
    @JsonProperty("paymentKey")
    private String paymentKey;
    
    @JsonProperty("orderId")
    private String orderId;
    
    @JsonProperty("orderName")
    private String orderName;
    
    @JsonProperty("taxExemptionAmount")
    private Long taxExemptionAmount;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("requestedAt")
    private String requestedAt;
    
    @JsonProperty("approvedAt")
    private String approvedAt;
    
    @JsonProperty("useEscrow")
    private Boolean useEscrow;
    
    @JsonProperty("cultureExpense")
    private Boolean cultureExpense;
    
    @JsonProperty("card")
    private CardInfo card;
    
    @Data
    public static class CardInfo {
        @JsonProperty("issuerCode")
        private String issuerCode;
        
        @JsonProperty("acquirerCode")
        private String acquirerCode;
        
        @JsonProperty("number")
        private String number;
        
        @JsonProperty("installmentPlanMonths")
        private Integer installmentPlanMonths;
        
        @JsonProperty("isInterestFree")
        private Boolean isInterestFree;
        
        @JsonProperty("interestPayer")
        private String interestPayer;
        
        @JsonProperty("approveNo")
        private String approveNo;
        
        @JsonProperty("useCardPoint")
        private Boolean useCardPoint;
        
        @JsonProperty("cardType")
        private String cardType;
        
        @JsonProperty("ownerType")
        private String ownerType;
    }
}