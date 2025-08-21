package ac.su.kdt.bepaymentservice.service;

import ac.su.kdt.bepaymentservice.entity.PaymentTransaction;
import ac.su.kdt.bepaymentservice.entity.Subscription;
import ac.su.kdt.bepaymentservice.kafka.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ac.su.kdt.bepaymentservice.metrics.PaymentMetrics;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentEventService {
    
    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;
    private final PaymentMetrics paymentMetrics;
    
    @Value("${kafka.topic.payment-events}")
    private String paymentEventsTopic;
    
    @Value("${kafka.topic.subscription-events}")
    private String subscriptionEventsTopic;
    
    public void publishSubscriptionCreated(Subscription subscription) {
        Map<String, Object> data = new HashMap<>();
        data.put("subscriptionId", subscription.getId());
        data.put("planId", subscription.getPlan().getId());
        data.put("planType", subscription.getPlan().getPlanType().name());
        data.put("billingCycle", subscription.getBillingCycle().name());
        data.put("amount", subscription.getAmount());
        data.put("currency", subscription.getCurrency());
        data.put("status", subscription.getStatus().name());
        
        PaymentEvent event = PaymentEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType(PaymentEvent.EventType.SUBSCRIPTION_CREATED.name())
            .timestamp(LocalDateTime.now())
            .userId(subscription.getUserId())
            .teamId(subscription.getTeamId())
            .data(data)
            .build();
        
        kafkaTemplate.send(subscriptionEventsTopic, event);
        paymentMetrics.incrementKafkaEventPublished("subscription.created");
        log.info("Published subscription created event for user: {}", subscription.getUserId());
    }
    
    public void publishSubscriptionCancelled(Subscription subscription) {
        Map<String, Object> data = new HashMap<>();
        data.put("subscriptionId", subscription.getId());
        data.put("cancelAtPeriodEnd", subscription.getCancelAtPeriodEnd());
        data.put("canceledAt", subscription.getCanceledAt());
        
        PaymentEvent event = PaymentEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType(PaymentEvent.EventType.SUBSCRIPTION_CANCELLED.name())
            .timestamp(LocalDateTime.now())
            .userId(subscription.getUserId())
            .teamId(subscription.getTeamId())
            .data(data)
            .build();
        
        kafkaTemplate.send(subscriptionEventsTopic, event);
        log.info("Published subscription cancelled event for user: {}", subscription.getUserId());
    }
    
    public void publishSubscriptionExpired(Subscription subscription) {
        Map<String, Object> data = new HashMap<>();
        data.put("subscriptionId", subscription.getId());
        data.put("expiredAt", subscription.getCurrentPeriodEnd());
        
        PaymentEvent event = PaymentEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType(PaymentEvent.EventType.SUBSCRIPTION_EXPIRED.name())
            .timestamp(LocalDateTime.now())
            .userId(subscription.getUserId())
            .teamId(subscription.getTeamId())
            .data(data)
            .build();
        
        kafkaTemplate.send(subscriptionEventsTopic, event);
        log.info("Published subscription expired event for user: {}", subscription.getUserId());
    }
    
    public void publishSubscriptionExpiring(Subscription subscription, int daysBeforeExpiry) {
        Map<String, Object> data = new HashMap<>();
        data.put("subscriptionId", subscription.getId());
        data.put("expiresAt", subscription.getCurrentPeriodEnd());
        data.put("daysBeforeExpiry", daysBeforeExpiry);
        
        PaymentEvent event = PaymentEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType(PaymentEvent.EventType.SUBSCRIPTION_EXPIRING.name())
            .timestamp(LocalDateTime.now())
            .userId(subscription.getUserId())
            .teamId(subscription.getTeamId())
            .data(data)
            .build();
        
        kafkaTemplate.send(subscriptionEventsTopic, event);
        log.info("Published subscription expiring event for user: {} (expires in {} days)", 
                subscription.getUserId(), daysBeforeExpiry);
    }
    
    public void publishSubscriptionStatusUpdated(Subscription subscription) {
        Map<String, Object> data = new HashMap<>();
        data.put("subscriptionId", subscription.getId());
        data.put("status", subscription.getStatus().name());
        data.put("updatedAt", LocalDateTime.now());
        
        PaymentEvent event = PaymentEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType(PaymentEvent.EventType.SUBSCRIPTION_STATUS_UPDATED.name())
            .timestamp(LocalDateTime.now())
            .userId(subscription.getUserId())
            .teamId(subscription.getTeamId())
            .data(data)
            .build();
        
        kafkaTemplate.send(subscriptionEventsTopic, event);
        log.info("Published subscription status updated event for user: {}", subscription.getUserId());
    }
    
    public void publishPaymentSucceeded(PaymentTransaction transaction) {
        Map<String, Object> data = new HashMap<>();
        data.put("transactionId", transaction.getId());
        data.put("subscriptionId", transaction.getSubscription().getId());
        data.put("amount", transaction.getAmount());
        data.put("currency", transaction.getCurrency());
        data.put("paymentMethod", transaction.getPaymentMethod().name());
        data.put("stripePaymentIntentId", transaction.getStripePaymentIntentId());
        
        PaymentEvent event = PaymentEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType(PaymentEvent.EventType.PAYMENT_SUCCEEDED.name())
            .timestamp(LocalDateTime.now())
            .userId(transaction.getSubscription().getUserId())
            .teamId(transaction.getSubscription().getTeamId())
            .data(data)
            .build();
        
        kafkaTemplate.send(paymentEventsTopic, event);
        log.info("Published payment succeeded event for transaction: {}", transaction.getId());
    }
    
    public void publishPaymentFailed(PaymentTransaction transaction) {
        Map<String, Object> data = new HashMap<>();
        data.put("transactionId", transaction.getId());
        data.put("subscriptionId", transaction.getSubscription().getId());
        data.put("amount", transaction.getAmount());
        data.put("currency", transaction.getCurrency());
        data.put("failureReason", transaction.getFailureReason());
        data.put("stripePaymentIntentId", transaction.getStripePaymentIntentId());
        
        PaymentEvent event = PaymentEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType(PaymentEvent.EventType.PAYMENT_FAILED.name())
            .timestamp(LocalDateTime.now())
            .userId(transaction.getSubscription().getUserId())
            .teamId(transaction.getSubscription().getTeamId())
            .data(data)
            .build();
        
        kafkaTemplate.send(paymentEventsTopic, event);
        log.info("Published payment failed event for transaction: {}", transaction.getId());
    }
    
    public void publishTicketsUsed(Long userId, int ticketsUsed, int remainingBalance) {
        Map<String, Object> data = new HashMap<>();
        data.put("ticketsUsed", ticketsUsed);
        data.put("remainingBalance", remainingBalance);
        
        PaymentEvent event = PaymentEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType(PaymentEvent.EventType.TICKETS_USED.name())
            .timestamp(LocalDateTime.now())
            .userId(userId)
            .data(data)
            .build();
        
        kafkaTemplate.send(paymentEventsTopic, event);
        log.info("Published tickets used event for user: {}", userId);
    }
    
    public void publishTicketsRefunded(Long userId, int ticketsRefunded, int newBalance) {
        Map<String, Object> data = new HashMap<>();
        data.put("ticketsRefunded", ticketsRefunded);
        data.put("newBalance", newBalance);
        
        PaymentEvent event = PaymentEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType(PaymentEvent.EventType.TICKETS_REFUNDED.name())
            .timestamp(LocalDateTime.now())
            .userId(userId)
            .data(data)
            .build();
        
        kafkaTemplate.send(paymentEventsTopic, event);
        log.info("Published tickets refunded event for user: {}", userId);
    }
    
    public void publishTicketsRefilled(Long userId, int ticketsAdded, int newBalance) {
        Map<String, Object> data = new HashMap<>();
        data.put("ticketsAdded", ticketsAdded);
        data.put("newBalance", newBalance);
        
        PaymentEvent event = PaymentEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType(PaymentEvent.EventType.TICKETS_REFILLED.name())
            .timestamp(LocalDateTime.now())
            .userId(userId)
            .data(data)
            .build();
        
        kafkaTemplate.send(paymentEventsTopic, event);
        log.info("Published tickets refilled event for user: {}", userId);
    }
    
    public void publishTicketBalanceLow(Long userId, int currentBalance, int threshold) {
        Map<String, Object> data = new HashMap<>();
        data.put("currentBalance", currentBalance);
        data.put("threshold", threshold);
        
        PaymentEvent event = PaymentEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType(PaymentEvent.EventType.TICKET_BALANCE_LOW.name())
            .timestamp(LocalDateTime.now())
            .userId(userId)
            .data(data)
            .build();
        
        kafkaTemplate.send(paymentEventsTopic, event);
        log.info("Published ticket balance low event for user: {}", userId);
    }
}