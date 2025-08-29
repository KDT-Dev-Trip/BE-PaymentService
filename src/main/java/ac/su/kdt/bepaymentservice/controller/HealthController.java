package ac.su.kdt.bepaymentservice.controller;

import ac.su.kdt.bepaymentservice.dto.HealthCheckResponse;
import ac.su.kdt.bepaymentservice.service.HealthCheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DevTrip 표준 헬스체크 컨트롤러
 * Gateway에서 PaymentService 상태를 확인하기 위한 엔드포인트
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HealthController {
    
    private final HealthCheckService healthCheckService;
    
    /**
     * 표준 헬스체크 엔드포인트 (신규)
     */
    @GetMapping("/health")
    public ResponseEntity<HealthCheckResponse> health() {
        try {
            HealthCheckResponse response = healthCheckService.performHealthCheck();
            
            // Payment 서비스 특화 정보 추가
            Map<String, Object> details = response.getDetails();
            if (details == null) {
                details = Map.of();
            }
            details.put("features", Map.of(
                "subscriptions", "enabled",
                "tickets", "enabled", 
                "stripe", "enabled",
                "kafka", "enabled",
                "gateway-auth", "enabled"
            ));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(503).body(
                HealthCheckResponse.builder()
                    .status("DOWN")
                    .service("BE-payment-service")
                    .version("1.0.0")
                    .timestamp(LocalDateTime.now())
                    .details(Map.of("error", e.getMessage()))
                    .build()
            );
        }
    }
    
    /**
     * 기존 헬스체크 엔드포인트 (하위 호환성)
     */
    @GetMapping("/v1/health")
    public ResponseEntity<Map<String, Object>> healthV1() {
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
    
    @GetMapping("/v1/info")
    public ResponseEntity<Map<String, Object>> info() {
        return ResponseEntity.ok(Map.of(
            "service", "DevOps Platform Payment Service",
            "description", "결제, 구독, 티켓 관리 서비스",
            "endpoints", Map.of(
                "subscriptions", "/api/subscriptions",
                "tickets", "/api/tickets",
                "health", "/api/health",
                "healthV1", "/api/v1/health"
            ),
            "authentication", "Gateway-based authentication with X-User-Id and X-User-Email headers"
        ));
    }
}