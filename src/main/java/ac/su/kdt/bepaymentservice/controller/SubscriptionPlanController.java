package ac.su.kdt.bepaymentservice.controller;

import ac.su.kdt.bepaymentservice.dto.SubscriptionPlanDto;
import ac.su.kdt.bepaymentservice.entity.SubscriptionPlan;
import ac.su.kdt.bepaymentservice.repository.SubscriptionPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/subscription-plans")
@RequiredArgsConstructor
@Slf4j
public class SubscriptionPlanController {
    
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    
    @GetMapping
    public ResponseEntity<List<SubscriptionPlanDto>> getAllActivePlans() {
        try {
            List<SubscriptionPlan> plans = subscriptionPlanRepository.findByIsActiveTrue();
            List<SubscriptionPlanDto> planDtos = plans.stream()
                .map(SubscriptionPlanDto::fromEntity)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(planDtos);
        } catch (Exception e) {
            log.error("Error fetching subscription plans", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/{planId}")
    public ResponseEntity<SubscriptionPlanDto> getPlan(@PathVariable Long planId) {
        try {
            return subscriptionPlanRepository.findById(planId)
                .filter(SubscriptionPlan::getIsActive)
                .map(SubscriptionPlanDto::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error fetching subscription plan: {}", planId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/types/{planType}")
    public ResponseEntity<SubscriptionPlanDto> getPlanByType(@PathVariable SubscriptionPlan.PlanType planType) {
        try {
            return subscriptionPlanRepository.findByPlanTypeAndIsActiveTrue(planType)
                .map(SubscriptionPlanDto::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error fetching subscription plan by type: {}", planType, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}