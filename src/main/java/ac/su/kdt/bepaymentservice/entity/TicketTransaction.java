package ac.su.kdt.bepaymentservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "ticket_transaction")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketTransaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TicketTransactionType transactionType;
    
    @Column(name = "ticket_amount", nullable = false)
    private Integer ticketAmount;
    
    @Column(name = "balance_before", nullable = false)
    private Integer balanceBefore;
    
    @Column(name = "balance_after", nullable = false)
    private Integer balanceAfter;
    
    @Column(name = "related_attempt_id")
    private Long relatedAttemptId;
    
    @Column(name = "reason", length = 500)
    private String reason;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    public enum TicketTransactionType {
        EARNED,
        SPENT,
        REFUND,
        ADMIN_ADJUST
    }
}