package ac.su.kdt.bepaymentservice.repository;

import ac.su.kdt.bepaymentservice.entity.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Long> {
    
    List<SubscriptionPlan> findByIsActiveTrue();
    
    Optional<SubscriptionPlan> findByPlanNameAndIsActiveTrue(String planName);
    
    Optional<SubscriptionPlan> findByPlanTypeAndIsActiveTrue(SubscriptionPlan.PlanType planType);
    
    Optional<SubscriptionPlan> findByStripeProductId(String stripeProductId);
    
    @Query("SELECT sp FROM SubscriptionPlan sp WHERE sp.stripePriceIdMonthly = :priceId OR sp.stripePriceIdYearly = :priceId")
    Optional<SubscriptionPlan> findByStripePriceId(@Param("priceId") String priceId);
    
    List<SubscriptionPlan> findByPlanTypeInAndIsActiveTrue(List<SubscriptionPlan.PlanType> planTypes);
}