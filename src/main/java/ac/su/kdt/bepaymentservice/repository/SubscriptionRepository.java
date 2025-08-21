package ac.su.kdt.bepaymentservice.repository;

import ac.su.kdt.bepaymentservice.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    
    Optional<Subscription> findByUserIdAndStatus(Long userId, Subscription.SubscriptionStatus status);
    
    @Query("SELECT s FROM Subscription s WHERE s.userId = :userId AND s.status IN :statuses ORDER BY s.createdAt DESC")
    List<Subscription> findByUserIdAndStatusInOrderByCreatedAtDesc(@Param("userId") Long userId, 
                                                                   @Param("statuses") List<Subscription.SubscriptionStatus> statuses);
    
    Optional<Subscription> findByUserIdAndStatusIn(Long userId, List<Subscription.SubscriptionStatus> statuses);
    
    Optional<Subscription> findByStripeSubscriptionId(String stripeSubscriptionId);
    
    List<Subscription> findByTeamIdAndStatus(Long teamId, Subscription.SubscriptionStatus status);
    
    @Query("SELECT s FROM Subscription s WHERE s.currentPeriodEnd <= :endDate AND s.status = :status")
    List<Subscription> findExpiringSubscriptions(@Param("endDate") LocalDateTime endDate, 
                                                 @Param("status") Subscription.SubscriptionStatus status);
    
    @Query("SELECT s FROM Subscription s WHERE s.status = :status AND s.currentPeriodEnd < :now")
    List<Subscription> findExpiredSubscriptions(@Param("status") Subscription.SubscriptionStatus status, 
                                               @Param("now") LocalDateTime now);
    
    List<Subscription> findByStripeCustomerId(String stripeCustomerId);
    
    @Query("SELECT COUNT(s) FROM Subscription s WHERE s.plan.id = :planId AND s.status = :status")
    Long countByPlanIdAndStatus(@Param("planId") Long planId, @Param("status") Subscription.SubscriptionStatus status);
    
    @Query("SELECT s FROM Subscription s WHERE s.userId = :userId ORDER BY s.createdAt DESC")
    List<Subscription> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);
}