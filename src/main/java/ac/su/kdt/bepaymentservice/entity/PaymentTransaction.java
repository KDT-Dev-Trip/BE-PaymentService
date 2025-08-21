package ac.su.kdt.bepaymentservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_transaction")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;
    
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
    
    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "KRW";
    
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_status", nullable = false)
    @Builder.Default
    private TransactionStatus transactionStatus = TransactionStatus.PENDING;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TransactionType transactionType;
    
    @Column(name = "stripe_payment_intent_id", length = 100)
    private String stripePaymentIntentId;
    
    @Column(name = "stripe_invoice_id", length = 100)
    private String stripeInvoiceId;
    
    @Column(name = "stripe_charge_id", length = 100)
    private String stripeChargeId;
    
    @Column(name = "external_transaction_id", length = 255)
    private String externalTransactionId;
    
    @Column(name = "failure_reason", length = 500)
    private String failureReason;
    
    @Column(name = "description", length = 500)
    private String description;
    
    @Column(name = "metadata", columnDefinition = "JSON")
    private String metadata;
    
    @Column(name = "processed_at")
    private LocalDateTime processedAt;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    public enum PaymentMethod {
        CARD,
        BANK_TRANSFER,
        DIGITAL_WALLET,
        OTHER
    }
    
    public enum TransactionStatus {
        PENDING,
        PROCESSING,
        SUCCEEDED,
        FAILED,
        CANCELED,
        REFUNDED,
        PARTIALLY_REFUNDED
    }
    
    public enum TransactionType {
        SUBSCRIPTION_PAYMENT,
        SETUP_FEE,
        UPGRADE,
        DOWNGRADE,
        REFUND,
        PARTIAL_REFUND,
        CHARGEBACK
    }
    
    public boolean isSuccessful() {
        return transactionStatus == TransactionStatus.SUCCEEDED;
    }
    
    public boolean isFailed() {
        return transactionStatus == TransactionStatus.FAILED || 
               transactionStatus == TransactionStatus.CANCELED;
    }
}