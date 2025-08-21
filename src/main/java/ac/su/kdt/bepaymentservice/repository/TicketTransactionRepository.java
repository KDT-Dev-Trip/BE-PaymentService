package ac.su.kdt.bepaymentservice.repository;

import ac.su.kdt.bepaymentservice.entity.TicketTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TicketTransactionRepository extends JpaRepository<TicketTransaction, Long> {
    
    List<TicketTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    List<TicketTransaction> findByUserIdAndTransactionType(Long userId, TicketTransaction.TicketTransactionType transactionType);
    
    @Query("SELECT tt FROM TicketTransaction tt WHERE tt.userId = :userId AND tt.createdAt BETWEEN :startDate AND :endDate ORDER BY tt.createdAt DESC")
    List<TicketTransaction> findByUserIdAndCreatedAtBetween(@Param("userId") Long userId,
                                                           @Param("startDate") LocalDateTime startDate,
                                                           @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT SUM(tt.ticketAmount) FROM TicketTransaction tt WHERE tt.userId = :userId AND tt.transactionType = :transactionType AND tt.createdAt BETWEEN :startDate AND :endDate")
    Integer sumTicketAmountByUserIdAndTransactionTypeAndDateRange(@Param("userId") Long userId,
                                                                 @Param("transactionType") TicketTransaction.TicketTransactionType transactionType,
                                                                 @Param("startDate") LocalDateTime startDate,
                                                                 @Param("endDate") LocalDateTime endDate);
    
    List<TicketTransaction> findByRelatedAttemptId(Long attemptId);
    
    @Query("SELECT COUNT(tt) FROM TicketTransaction tt WHERE tt.userId = :userId AND tt.transactionType = :transactionType AND tt.createdAt >= :startDate")
    Long countByUserIdAndTransactionTypeAndCreatedAtAfter(@Param("userId") Long userId,
                                                         @Param("transactionType") TicketTransaction.TicketTransactionType transactionType,
                                                         @Param("startDate") LocalDateTime startDate);
}