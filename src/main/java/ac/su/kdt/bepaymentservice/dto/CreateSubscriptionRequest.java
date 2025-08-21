package ac.su.kdt.bepaymentservice.dto;

import ac.su.kdt.bepaymentservice.entity.Subscription;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSubscriptionRequest {
    
    @NotNull(message = "User ID is required")
    @Positive(message = "User ID must be positive")
    private Long userId;
    
    private Long teamId;
    
    @NotNull(message = "Plan ID is required")
    @Positive(message = "Plan ID must be positive")
    private Long planId;
    
    @NotNull(message = "Billing cycle is required")
    private Subscription.BillingCycle billingCycle;
    
    private String stripePaymentMethodId;
    
    private String successUrl;
    
    private String cancelUrl;
    
    private boolean startTrial;
    
    private Integer trialDays;
}