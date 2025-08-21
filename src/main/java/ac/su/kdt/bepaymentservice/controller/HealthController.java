package ac.su.kdt.bepaymentservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 헬스체크 컨트롤러
 * Gateway에서 PaymentService 상태를 확인하기 위한 엔드포인트
 */
@RestController
@RequestMapping("/api/v1")
public class HealthController {
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "service", "payment-service",
            "status", "healthy",
            "timestamp", LocalDateTime.now(),
            "version", "1.0.0",
            "features", Map.of(
                "subscriptions", "enabled",
                "tickets", "enabled", 
                "stripe", "enabled",
                "kafka", "enabled",
                "gateway-auth", "enabled"
            )
        ));
    }
    
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        return ResponseEntity.ok(Map.of(
            "service", "DevOps Platform Payment Service",
            "description", "결제, 구독, 티켓 관리 서비스",
            "endpoints", Map.of(
                "subscriptions", "/api/v1/subscriptions",
                "tickets", "/api/v1/tickets",
                "health", "/api/v1/health"
            ),
            "authentication", "Gateway-based authentication with X-User-Id and X-User-Email headers"
        ));
    }
}