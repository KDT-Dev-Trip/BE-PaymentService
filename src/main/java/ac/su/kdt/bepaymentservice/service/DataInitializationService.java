package ac.su.kdt.bepaymentservice.service;

import ac.su.kdt.bepaymentservice.entity.SubscriptionPlan;
import ac.su.kdt.bepaymentservice.repository.SubscriptionPlanRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@org.springframework.context.annotation.Profile("!test")
public class DataInitializationService implements CommandLineRunner {
    
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final ObjectMapper objectMapper;
    
    @Override
    @Transactional
    public void run(String... args) {
        initializeSubscriptionPlans();
    }
    
    private void initializeSubscriptionPlans() {
        if (subscriptionPlanRepository.count() > 0) {
            log.info("Subscription plans already exist, skipping initialization");
            return;
        }
        
        log.info("Initializing subscription plans...");
        
        try {
            // Economy Class Plan
            SubscriptionPlan economyPlan = createEconomyClassPlan();
            subscriptionPlanRepository.save(economyPlan);
            
            // Business Class Plan
            SubscriptionPlan businessPlan = createBusinessClassPlan();
            subscriptionPlanRepository.save(businessPlan);
            
            // First Class Plan
            SubscriptionPlan firstClassPlan = createFirstClassPlan();
            subscriptionPlanRepository.save(firstClassPlan);
            
            log.info("Successfully initialized {} subscription plans", 3);
            
        } catch (Exception e) {
            log.error("Error initializing subscription plans", e);
        }
    }
    
    private SubscriptionPlan createEconomyClassPlan() throws JsonProcessingException {
        Map<String, Object> features = new HashMap<>();
        features.put("maxTeamMembers", 2);
        features.put("maxMonthlyAttempts", 10);
        features.put("basicSupport", true);
        features.put("communityAccess", true);
        features.put("basicMissions", true);
        features.put("environmentTimeLimit", 4); // hours
        features.put("concurrentEnvironments", 1);
        
        return SubscriptionPlan.builder()
            .planName("Economy Class")
            .planType(SubscriptionPlan.PlanType.ECONOMY_CLASS)
            .monthlyPrice(new BigDecimal("29.00"))
            .yearlyPrice(new BigDecimal("290.00")) // 2 months free
            .maxTeamMembers(2)
            .maxMonthlyAttempts(10)
            .ticketLimit(3)
            .ticketRefillAmount(3)
            .ticketRefillIntervalHours(24)
            .features(objectMapper.writeValueAsString(features))
            .description("Perfect for individual learners and small teams getting started with DevOps")
            .isActive(true)
            .build();
    }
    
    private SubscriptionPlan createBusinessClassPlan() throws JsonProcessingException {
        Map<String, Object> features = new HashMap<>();
        features.put("maxTeamMembers", 6);
        features.put("maxMonthlyAttempts", 50);
        features.put("prioritySupport", true);
        features.put("communityAccess", true);
        features.put("advancedMissions", true);
        features.put("customMissions", true);
        features.put("environmentTimeLimit", 8); // hours
        features.put("concurrentEnvironments", 3);
        features.put("teamAnalytics", true);
        features.put("aiCodeReviews", true);
        
        return SubscriptionPlan.builder()
            .planName("Business Class")
            .planType(SubscriptionPlan.PlanType.BUSINESS_CLASS)
            .monthlyPrice(new BigDecimal("79.00"))
            .yearlyPrice(new BigDecimal("790.00")) // 2 months free
            .maxTeamMembers(6)
            .maxMonthlyAttempts(50)
            .ticketLimit(8)
            .ticketRefillAmount(5)
            .ticketRefillIntervalHours(12)
            .features(objectMapper.writeValueAsString(features))
            .description("Ideal for growing teams and organizations serious about DevOps training")
            .isActive(true)
            .build();
    }
    
    private SubscriptionPlan createFirstClassPlan() throws JsonProcessingException {
        Map<String, Object> features = new HashMap<>();
        features.put("maxTeamMembers", 20);
        features.put("maxMonthlyAttempts", 200);
        features.put("dedicatedSupport", true);
        features.put("communityAccess", true);
        features.put("allMissions", true);
        features.put("customMissions", true);
        features.put("privateMissions", true);
        features.put("environmentTimeLimit", 24); // hours
        features.put("concurrentEnvironments", 10);
        features.put("teamAnalytics", true);
        features.put("aiCodeReviews", true);
        features.put("customReports", true);
        features.put("ssoIntegration", true);
        features.put("apiAccess", true);
        features.put("dedicatedInfrastructure", true);
        
        return SubscriptionPlan.builder()
            .planName("First Class")
            .planType(SubscriptionPlan.PlanType.FIRST_CLASS)
            .monthlyPrice(new BigDecimal("199.00"))
            .yearlyPrice(new BigDecimal("1990.00")) // 2 months free
            .maxTeamMembers(20)
            .maxMonthlyAttempts(200)
            .ticketLimit(15)
            .ticketRefillAmount(10)
            .ticketRefillIntervalHours(8)
            .features(objectMapper.writeValueAsString(features))
            .description("Enterprise-grade solution for large organizations with comprehensive DevOps training needs")
            .isActive(true)
            .build();
    }
}