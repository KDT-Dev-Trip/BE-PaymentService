package ac.su.kdt.bepaymentservice.repository;

import ac.su.kdt.bepaymentservice.entity.BillingKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BillingKeyRepository extends JpaRepository<BillingKey, Long> {
    Optional<BillingKey> findByCustomerKey(String customerKey);
    Optional<BillingKey> findByBillingKey(String billingKey);
    boolean existsByCustomerKey(String customerKey);
    boolean existsByBillingKey(String billingKey);
}