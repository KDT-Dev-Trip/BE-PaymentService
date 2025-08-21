package ac.su.kdt.bepaymentservice.repository;

import ac.su.kdt.bepaymentservice.entity.UserTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserTicketRepository extends JpaRepository<UserTicket, Long> {
    
    Optional<UserTicket> findByUserId(Long userId);
    
    @Query("SELECT ut FROM UserTicket ut WHERE ut.nextRefillAt <= :now")
    List<UserTicket> findUsersEligibleForRefill(@Param("now") LocalDateTime now);
    
    @Query("SELECT COUNT(ut) FROM UserTicket ut WHERE ut.currentTickets >= :minTickets")
    Long countUsersWithMinimumTickets(@Param("minTickets") Integer minTickets);
    
    @Query("SELECT AVG(ut.currentTickets) FROM UserTicket ut")
    Double getAverageTicketBalance();
    
    @Query("SELECT ut FROM UserTicket ut WHERE ut.currentTickets = 0")
    List<UserTicket> findUsersWithZeroTickets();
}