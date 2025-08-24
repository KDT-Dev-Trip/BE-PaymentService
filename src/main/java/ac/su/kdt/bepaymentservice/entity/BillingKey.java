package ac.su.kdt.bepaymentservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "billing_keys")
@Getter
@Setter
@NoArgsConstructor
public class BillingKey {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_key", nullable = false, unique = true)
    private String customerKey;

    @Column(name = "billing_key", nullable = false, unique = true)
    private String billingKey;

    @Column(name = "card_number")
    private String cardNumber;

    @Column(name = "card_type")
    private String cardType;

    @Column(name = "card_company")
    private String cardCompany;

    @Column(name = "owner_type")
    private String ownerType;

    @Column(name = "authenticated_at")
    private LocalDateTime authenticatedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}