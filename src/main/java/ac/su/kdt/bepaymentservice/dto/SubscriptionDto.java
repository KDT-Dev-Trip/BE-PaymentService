package ac.su.kdt.bepaymentservice.dto;

import ac.su.kdt.bepaymentservice.entity.Subscription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionDto {
    private Long id;
    private Long userId;
    private Long teamId;
    private SubscriptionPlanDto plan;
    private Subscription.SubscriptionStatus status;
    private Subscription.BillingCycle billingCycle;
    private BigDecimal amount;
    private String currency;
    private String stripeSubscriptionId;
    private String stripeCustomerId;
    private LocalDateTime currentPeriodStart;
    private LocalDateTime currentPeriodEnd;
    private LocalDateTime trialStart;
    private LocalDateTime trialEnd;
    private Boolean cancelAtPeriodEnd;
    private LocalDateTime canceledAt;
    private Boolean autoRenewal;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static SubscriptionDto fromEntity(Subscription entity) {
        return SubscriptionDto.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .teamId(entity.getTeamId())
                .plan(entity.getPlan() != null ? SubscriptionPlanDto.fromEntity(entity.getPlan()) : null)
                .status(entity.getStatus())
                .billingCycle(entity.getBillingCycle())
                .amount(entity.getAmount())
                .currency(entity.getCurrency())
                .stripeSubscriptionId(entity.getStripeSubscriptionId())
                .stripeCustomerId(entity.getStripeCustomerId())
                .currentPeriodStart(entity.getCurrentPeriodStart())
                .currentPeriodEnd(entity.getCurrentPeriodEnd())
                .trialStart(entity.getTrialStart())
                .trialEnd(entity.getTrialEnd())
                .cancelAtPeriodEnd(entity.getCancelAtPeriodEnd())
                .canceledAt(entity.getCanceledAt())
                .autoRenewal(entity.getAutoRenewal())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}