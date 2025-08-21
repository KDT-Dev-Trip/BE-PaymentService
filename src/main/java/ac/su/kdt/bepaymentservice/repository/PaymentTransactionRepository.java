package ac.su.kdt.bepaymentservice.repository;

import ac.su.kdt.bepaymentservice.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    
    List<PaymentTransaction> findBySubscriptionIdOrderByCreatedAtDesc(Long subscriptionId);
    
    Optional<PaymentTransaction> findByStripePaymentIntentId(String stripePaymentIntentId);
    
    Optional<PaymentTransaction> findByStripeInvoiceId(String stripeInvoiceId);
    
    Optional<PaymentTransaction> findByStripeChargeId(String stripeChargeId);
    
    List<PaymentTransaction> findByTransactionStatus(PaymentTransaction.TransactionStatus status);
    
    @Query("SELECT pt FROM PaymentTransaction pt WHERE pt.subscription.userId = :userId ORDER BY pt.createdAt DESC")
    List<PaymentTransaction> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);
    
    @Query("SELECT pt FROM PaymentTransaction pt WHERE pt.createdAt BETWEEN :startDate AND :endDate")
    List<PaymentTransaction> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, 
                                                   @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT pt FROM PaymentTransaction pt WHERE pt.subscription.userId = :userId AND pt.createdAt BETWEEN :startDate AND :endDate")
    List<PaymentTransaction> findByUserIdAndCreatedAtBetween(@Param("userId") Long userId,
                                                            @Param("startDate") LocalDateTime startDate,
                                                            @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT SUM(pt.amount) FROM PaymentTransaction pt WHERE pt.transactionStatus = :status AND pt.createdAt BETWEEN :startDate AND :endDate")
    Double calculateTotalAmountByStatusAndDateRange(@Param("status") PaymentTransaction.TransactionStatus status,
                                                   @Param("startDate") LocalDateTime startDate,
                                                   @Param("endDate") LocalDateTime endDate);
    
    List<PaymentTransaction> findByTransactionTypeAndTransactionStatus(PaymentTransaction.TransactionType type,
                                                                      PaymentTransaction.TransactionStatus status);
}