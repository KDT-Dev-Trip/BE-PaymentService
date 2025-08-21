package ac.su.kdt.bepaymentservice.service;

import ac.su.kdt.bepaymentservice.dto.CreateSubscriptionRequest;
import ac.su.kdt.bepaymentservice.dto.SubscriptionDto;
import ac.su.kdt.bepaymentservice.entity.Subscription;
import ac.su.kdt.bepaymentservice.entity.SubscriptionPlan;
import ac.su.kdt.bepaymentservice.repository.SubscriptionPlanRepository;
import ac.su.kdt.bepaymentservice.repository.SubscriptionRepository;
import ac.su.kdt.bepaymentservice.metrics.PaymentMetrics;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.checkout.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SubscriptionService {
    
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final StripeService stripeService;
    private final PaymentEventService paymentEventService;
    private final PaymentMetrics paymentMetrics;
    
    public SubscriptionDto createSubscription(CreateSubscriptionRequest request) {
        log.info("Creating subscription for user: {} with plan: {}", request.getUserId(), request.getPlanId());
        
        var timer = paymentMetrics.startSubscriptionTimer();
        
        // Check if user already has an active subscription
        List<Subscription.SubscriptionStatus> activeStatuses = List.of(
            Subscription.SubscriptionStatus.ACTIVE,
            Subscription.SubscriptionStatus.TRIAL
        );
        
        List<Subscription> existingActiveSubscriptions = subscriptionRepository
            .findByUserIdAndStatusInOrderByCreatedAtDesc(request.getUserId(), activeStatuses);
        if (!existingActiveSubscriptions.isEmpty()) {
            throw new IllegalStateException("User already has an active subscription");
        }
        
        // Get subscription plan
        SubscriptionPlan plan = subscriptionPlanRepository.findById(request.getPlanId())
            .orElseThrow(() -> new IllegalArgumentException("Subscription plan not found"));
        
        if (!plan.getIsActive()) {
            throw new IllegalArgumentException("Subscription plan is not active");
        }
        
        // Calculate amount based on billing cycle
        BigDecimal amount = request.getBillingCycle() == Subscription.BillingCycle.YEARLY
            ? plan.getYearlyPrice() : plan.getMonthlyPrice();
        
        // Create subscription entity
        Subscription subscription = Subscription.builder()
            .userId(request.getUserId())
            .teamId(request.getTeamId())
            .plan(plan)
            .status(request.isStartTrial() ? Subscription.SubscriptionStatus.TRIAL : Subscription.SubscriptionStatus.INCOMPLETE)
            .billingCycle(request.getBillingCycle())
            .amount(amount)
            .currency("KRW")
            .autoRenewal(true)
            .build();
        
        // Set trial dates if applicable
        if (request.isStartTrial() && request.getTrialDays() != null && request.getTrialDays() > 0) {
            LocalDateTime now = LocalDateTime.now();
            subscription.setTrialStart(now);
            subscription.setTrialEnd(now.plusDays(request.getTrialDays()));
            subscription.setCurrentPeriodStart(now);
            subscription.setCurrentPeriodEnd(now.plusDays(request.getTrialDays()));
        }
        
        subscription = subscriptionRepository.save(subscription);
        
        // Publish subscription created event
        paymentEventService.publishSubscriptionCreated(subscription);
        
        log.info("Created subscription: {} for user: {}", subscription.getId(), request.getUserId());
        
        paymentMetrics.incrementSubscriptionSuccess();
        paymentMetrics.recordSubscriptionProcessingTime(timer);
        
        return SubscriptionDto.fromEntity(subscription);
    }
    
    @Transactional
    public SubscriptionDto activateSubscription(Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
            .orElseThrow(() -> new IllegalArgumentException("Subscription not found"));
        
        subscription.setStatus(Subscription.SubscriptionStatus.ACTIVE);
        subscription.setCurrentPeriodStart(LocalDateTime.now());
        subscription.setCurrentPeriodEnd(LocalDateTime.now().plusMonths(
            subscription.getBillingCycle() == Subscription.BillingCycle.YEARLY ? 12 : 1
        ));
        
        subscription = subscriptionRepository.save(subscription);
        
        // Publish subscription activated event
        paymentEventService.publishSubscriptionCreated(subscription);
        
        log.info("Activated subscription: {} for user: {}", subscription.getId(), subscription.getUserId());
        return SubscriptionDto.fromEntity(subscription);
    }
    
    public String createCheckoutSession(CreateSubscriptionRequest request) throws StripeException {
        log.info("Creating checkout session for user: {} with plan: {}", request.getUserId(), request.getPlanId());
        
        SubscriptionPlan plan = subscriptionPlanRepository.findById(request.getPlanId())
            .orElseThrow(() -> new IllegalArgumentException("Subscription plan not found"));
        
        String priceId = request.getBillingCycle() == Subscription.BillingCycle.YEARLY
            ? plan.getStripePriceIdYearly() : plan.getStripePriceIdMonthly();
        
        if (priceId == null) {
            throw new IllegalStateException("Stripe price ID not configured for this plan and billing cycle");
        }
        
        // Create or get customer
        String customerId = getOrCreateStripeCustomer(request.getUserId());
        
        Session session = stripeService.createCheckoutSession(
            customerId, 
            priceId, 
            request.getSuccessUrl(), 
            request.getCancelUrl(), 
            request.getUserId()
        );
        
        return session.getUrl();
    }
    
    public SubscriptionDto getUserActiveSubscription(Long userId) {
        List<Subscription.SubscriptionStatus> activeStatuses = List.of(
            Subscription.SubscriptionStatus.ACTIVE,
            Subscription.SubscriptionStatus.TRIAL
        );
        
        List<Subscription> activeSubscriptions = subscriptionRepository
            .findByUserIdAndStatusInOrderByCreatedAtDesc(userId, activeStatuses);
        return activeSubscriptions.isEmpty() ? null : SubscriptionDto.fromEntity(activeSubscriptions.get(0));
    }
    
    public List<SubscriptionDto> getUserSubscriptions(Long userId) {
        return subscriptionRepository.findByUserIdOrderByCreatedAtDesc(userId)
            .stream()
            .map(SubscriptionDto::fromEntity)
            .collect(Collectors.toList());
    }
    
    public SubscriptionDto cancelSubscription(Long subscriptionId, boolean cancelAtPeriodEnd) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
            .orElseThrow(() -> new IllegalArgumentException("Subscription not found"));
        
        if (cancelAtPeriodEnd) {
            subscription.setCancelAtPeriodEnd(true);
        } else {
            subscription.setStatus(Subscription.SubscriptionStatus.CANCELED);
            subscription.setCanceledAt(LocalDateTime.now());
        }
        
        subscription = subscriptionRepository.save(subscription);
        
        // Publish subscription cancelled event
        paymentEventService.publishSubscriptionCancelled(subscription);
        
        log.info("Cancelled subscription: {} for user: {}", subscriptionId, subscription.getUserId());
        return SubscriptionDto.fromEntity(subscription);
    }
    
    public SubscriptionDto updateSubscriptionStatus(String stripeSubscriptionId, Subscription.SubscriptionStatus status) {
        Subscription subscription = subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId)
            .orElseThrow(() -> new IllegalArgumentException("Subscription not found"));
        
        subscription.setStatus(status);
        subscription = subscriptionRepository.save(subscription);
        
        // Publish subscription status updated event
        paymentEventService.publishSubscriptionStatusUpdated(subscription);
        
        log.info("Updated subscription status: {} to: {}", subscription.getId(), status);
        return SubscriptionDto.fromEntity(subscription);
    }
    
    private String getOrCreateStripeCustomer(Long userId) throws StripeException {
        // In a real implementation, you would fetch user details from user service
        // For now, we'll create a basic customer
        String email = "user" + userId + "@example.com";
        String name = "User " + userId;
        
        Customer customer = stripeService.createCustomer(email, name, userId);
        return customer.getId();
    }
    
    public void processExpiredSubscriptions() {
        LocalDateTime now = LocalDateTime.now();
        List<Subscription> expiredSubscriptions = subscriptionRepository.findExpiredSubscriptions(
            Subscription.SubscriptionStatus.ACTIVE, now
        );
        
        for (Subscription subscription : expiredSubscriptions) {
            subscription.setStatus(Subscription.SubscriptionStatus.EXPIRED);
            subscriptionRepository.save(subscription);
            
            paymentEventService.publishSubscriptionExpired(subscription);
            log.info("Expired subscription: {} for user: {}", subscription.getId(), subscription.getUserId());
        }
    }
    
    public void processExpiringSubscriptions(int daysBeforeExpiry) {
        LocalDateTime expiryDate = LocalDateTime.now().plusDays(daysBeforeExpiry);
        List<Subscription> expiringSubscriptions = subscriptionRepository.findExpiringSubscriptions(
            expiryDate, Subscription.SubscriptionStatus.ACTIVE
        );
        
        for (Subscription subscription : expiringSubscriptions) {
            paymentEventService.publishSubscriptionExpiring(subscription, daysBeforeExpiry);
            log.info("Subscription expiring in {} days: {} for user: {}", 
                    daysBeforeExpiry, subscription.getId(), subscription.getUserId());
        }
    }
}