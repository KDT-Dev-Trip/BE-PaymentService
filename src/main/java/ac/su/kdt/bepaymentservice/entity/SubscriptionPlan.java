package ac.su.kdt.bepaymentservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "subscription_plan")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionPlan {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "plan_name", nullable = false, unique = true, length = 100)
    private String planName;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "plan_type", nullable = false)
    private PlanType planType;
    
    @Column(name = "monthly_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal monthlyPrice;
    
    @Column(name = "yearly_price", precision = 10, scale = 2)
    private BigDecimal yearlyPrice;
    
    @Column(name = "stripe_price_id_monthly", length = 100)
    private String stripePriceIdMonthly;
    
    @Column(name = "stripe_price_id_yearly", length = 100)
    private String stripePriceIdYearly;
    
    @Column(name = "stripe_product_id", length = 100)
    private String stripeProductId;
    
    @Column(name = "max_team_members")
    private Integer maxTeamMembers;
    
    @Column(name = "max_monthly_attempts", nullable = false)
    private Integer maxMonthlyAttempts;
    
    @Column(name = "ticket_limit", nullable = false)
    private Integer ticketLimit;
    
    @Column(name = "ticket_refill_amount", nullable = false)
    private Integer ticketRefillAmount;
    
    @Column(name = "ticket_refill_interval_hours", nullable = false)
    private Integer ticketRefillIntervalHours;
    
    @Column(name = "features", columnDefinition = "JSON")
    private String features;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Subscription> subscriptions;
    
    public enum PlanType {
        ECONOMY_CLASS,
        BUSINESS_CLASS,
        FIRST_CLASS
    }
}