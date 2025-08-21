package ac.su.kdt.bepaymentservice.dto;

import ac.su.kdt.bepaymentservice.entity.UserTicket;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketDto {
    private Long id;
    private Long userId;
    private Integer currentTickets;
    private LocalDateTime lastTicketRefill;
    private LocalDateTime nextRefillAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static TicketDto fromEntity(UserTicket entity) {
        return TicketDto.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .currentTickets(entity.getCurrentTickets())
                .lastTicketRefill(entity.getLastTicketRefill())
                .nextRefillAt(entity.getNextRefillAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}