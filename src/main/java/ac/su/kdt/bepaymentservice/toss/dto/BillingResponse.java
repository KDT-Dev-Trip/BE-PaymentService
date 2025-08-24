package ac.su.kdt.bepaymentservice.toss.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class BillingResponse {
    @JsonProperty("mId")
    private String mId;
    
    @JsonProperty("customerKey")
    private String customerKey;
    
    @JsonProperty("authenticatedAt")
    private String authenticatedAt;
    
    @JsonProperty("method")
    private String method;
    
    @JsonProperty("billingKey")
    private String billingKey;
    
    @JsonProperty("card")
    private CardInfo card;
    
    @JsonProperty("cardCompany")
    private String cardCompany;
    
    @JsonProperty("cardNumber")
    private String cardNumber;
    
    @Data
    public static class CardInfo {
        @JsonProperty("issuerCode")
        private String issuerCode;
        
        @JsonProperty("acquirerCode")
        private String acquirerCode;
        
        @JsonProperty("number")
        private String number;
        
        @JsonProperty("cardType")
        private String cardType;
        
        @JsonProperty("ownerType")
        private String ownerType;
    }
}