package ac.su.kdt.bepaymentservice.controller;

import ac.su.kdt.bepaymentservice.entity.SubscriptionPlan;
import ac.su.kdt.bepaymentservice.repository.SubscriptionPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/test")
@RequiredArgsConstructor
@Slf4j
public class TestController {
    
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    
    @GetMapping("/plans")
    public ResponseEntity<List<SubscriptionPlan>> getPlans() {
        List<SubscriptionPlan> plans = subscriptionPlanRepository.findAll();
        return ResponseEntity.ok(plans);
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of("status", "OK", "service", "payment-service"));
    }
    
    
    
    
}