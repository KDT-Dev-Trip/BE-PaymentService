package ac.su.kdt.bepaymentservice.controller;

import ac.su.kdt.bepaymentservice.dto.TicketDto;
import ac.su.kdt.bepaymentservice.service.TicketService;
import ac.su.kdt.bepaymentservice.util.GatewayAuthUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
@Slf4j
public class TicketController {
    
    private final TicketService ticketService;
    
    @GetMapping("/users/{userId}")
    public ResponseEntity<TicketDto> getUserTickets(@PathVariable String userId) {
        try {
            // Gateway 인증 확인
            if (!GatewayAuthUtils.isAuthenticated()) {
                log.warn("Unauthorized access attempt to user tickets for user: {}", userId);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            // 본인의 티켓 정보만 조회 가능
            if (!GatewayAuthUtils.isCurrentUser(userId)) {
                log.warn("User {} attempted to access tickets of user {}", 
                        GatewayAuthUtils.getCurrentUserId(), userId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            log.info("Fetching tickets for authenticated user: {} ({})", 
                    GatewayAuthUtils.getCurrentUserInfo(), userId);
            
            // String userId를 Long으로 변환하여 서비스 호출
            Long userIdLong = convertUserIdToLong(userId);
            TicketDto tickets = ticketService.getUserTickets(userIdLong);
            return ResponseEntity.ok(tickets);
        } catch (Exception e) {
            log.error("Error fetching tickets for user: {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping("/users/{userId}/use")
    public ResponseEntity<Map<String, Object>> useTickets(
            @PathVariable String userId,
            @RequestParam int amount,
            @RequestParam(required = false) Long attemptId,
            @RequestParam(required = false) String reason) {
        try {
            // Gateway 인증 확인 및 권한 검증
            if (!GatewayAuthUtils.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", "Authentication required"
                ));
            }
            
            if (!GatewayAuthUtils.isCurrentUser(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "success", false,
                    "message", "Access denied"
                ));
            }
            
            Long userIdLong = convertUserIdToLong(userId);
            boolean success = ticketService.useTickets(userIdLong, amount, attemptId, reason);
            
            if (success) {
                TicketDto updatedTickets = ticketService.getUserTickets(userIdLong);
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Tickets used successfully",
                    "tickets", updatedTickets
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Not enough tickets available"
                ));
            }
        } catch (Exception e) {
            log.error("Error using tickets for user: {}", userId, e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Internal server error"
            ));
        }
    }
    
    @PostMapping("/users/{userId}/refund")
    public ResponseEntity<Map<String, Object>> refundTickets(
            @PathVariable String userId,
            @RequestParam int amount,
            @RequestParam(required = false) Long attemptId,
            @RequestParam(required = false) String reason) {
        try {
            // Gateway 인증 확인 및 권한 검증
            if (!GatewayAuthUtils.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", "Authentication required"
                ));
            }
            
            if (!GatewayAuthUtils.isCurrentUser(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "success", false,
                    "message", "Access denied"
                ));
            }
            
            Long userIdLong = convertUserIdToLong(userId);
            ticketService.refundTickets(userIdLong, amount, attemptId, reason);
            TicketDto updatedTickets = ticketService.getUserTickets(userIdLong);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Tickets refunded successfully",
                "tickets", updatedTickets
            ));
        } catch (Exception e) {
            log.error("Error refunding tickets for user: {}", userId, e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Internal server error"
            ));
        }
    }
    
    @PostMapping("/users/{userId}/adjust")
    public ResponseEntity<Map<String, Object>> adjustTickets(
            @PathVariable String userId,
            @RequestParam int adjustment,
            @RequestParam(required = false) String reason) {
        try {
            // Gateway 인증 확인 및 권한 검증
            if (!GatewayAuthUtils.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", "Authentication required"
                ));
            }
            
            if (!GatewayAuthUtils.isCurrentUser(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "success", false,
                    "message", "Access denied"
                ));
            }
            
            Long userIdLong = convertUserIdToLong(userId);
            ticketService.adjustTickets(userIdLong, adjustment, reason);
            TicketDto updatedTickets = ticketService.getUserTickets(userIdLong);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Tickets adjusted successfully",
                "tickets", updatedTickets
            ));
        } catch (Exception e) {
            log.error("Error adjusting tickets for user: {}", userId, e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Internal server error"
            ));
        }
    }
    
    @PostMapping("/refill")
    public ResponseEntity<Map<String, String>> processTicketRefills() {
        try {
            ticketService.processTicketRefills();
            return ResponseEntity.ok(Map.of(
                "success", "true",
                "message", "Ticket refills processed successfully"
            ));
        } catch (Exception e) {
            log.error("Error processing ticket refills", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", "false",
                "message", "Internal server error"
            ));
        }
    }
    
    /**
     * 사용자 ID를 UUID String에서 Long으로 변환
     * UUID의 hash 값을 Long으로 사용하여 기존 서비스와 호환성 유지
     */
    private Long convertUserIdToLong(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        
        // UUID 문자열의 해시코드를 Long으로 변환
        // 음수를 양수로 변환하기 위해 Math.abs 사용
        long hash = Math.abs((long) userId.hashCode());
        
        // Long 범위를 벗어나지 않도록 보정
        if (hash < 0) {
            hash = Math.abs(hash);
        }
        
        log.debug("Converted user ID {} to Long: {}", userId, hash);
        return hash;
    }
}