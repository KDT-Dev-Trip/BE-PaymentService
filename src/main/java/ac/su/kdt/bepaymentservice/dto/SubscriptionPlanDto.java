package ac.su.kdt.bepaymentservice.dto;

import ac.su.kdt.bepaymentservice.entity.SubscriptionPlan;
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
public class SubscriptionPlanDto {
    private Long id;
    private String planName;
    private SubscriptionPlan.PlanType planType;
    private BigDecimal monthlyPrice;
    private BigDecimal yearlyPrice;
    private String stripePriceIdMonthly;
    private String stripePriceIdYearly;
    private String stripeProductId;
    private Integer maxTeamMembers;
    private Integer maxMonthlyAttempts;
    private Integer ticketLimit;
    private Integer ticketRefillAmount;
    private Integer ticketRefillIntervalHours;
    private String features;
    private String description;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static SubscriptionPlanDto fromEntity(SubscriptionPlan entity) {
        return SubscriptionPlanDto.builder()
                .id(entity.getId())
                .planName(entity.getPlanName())
                .planType(entity.getPlanType())
                .monthlyPrice(entity.getMonthlyPrice())
                .yearlyPrice(entity.getYearlyPrice())
                .stripePriceIdMonthly(entity.getStripePriceIdMonthly())
                .stripePriceIdYearly(entity.getStripePriceIdYearly())
                .stripeProductId(entity.getStripeProductId())
                .maxTeamMembers(entity.getMaxTeamMembers())
                .maxMonthlyAttempts(entity.getMaxMonthlyAttempts())
                .ticketLimit(entity.getTicketLimit())
                .ticketRefillAmount(entity.getTicketRefillAmount())
                .ticketRefillIntervalHours(entity.getTicketRefillIntervalHours())
                .features(entity.getFeatures())
                .description(entity.getDescription())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}