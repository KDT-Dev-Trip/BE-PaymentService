package ac.su.kdt.bepaymentservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_ticket")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserTicket {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;
    
    @Column(name = "current_tickets", nullable = false)
    @Builder.Default
    private Integer currentTickets = 0;
    
    @Column(name = "last_ticket_refill")
    private LocalDateTime lastTicketRefill;
    
    @Column(name = "next_refill_at")
    private LocalDateTime nextRefillAt;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    public boolean hasEnoughTickets(int requiredTickets) {
        return currentTickets >= requiredTickets;
    }
    
    public void useTickets(int ticketsToUse) {
        if (currentTickets < ticketsToUse) {
            throw new IllegalStateException("Not enough tickets available");
        }
        this.currentTickets -= ticketsToUse;
    }
    
    public void addTickets(int ticketsToAdd) {
        this.currentTickets += ticketsToAdd;
    }
    
    public boolean isRefillDue() {
        return nextRefillAt != null && LocalDateTime.now().isAfter(nextRefillAt);
    }
}