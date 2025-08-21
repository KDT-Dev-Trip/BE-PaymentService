package ac.su.kdt.bepaymentservice.service;

import ac.su.kdt.bepaymentservice.entity.PaymentTransaction;
import ac.su.kdt.bepaymentservice.entity.Subscription;
import ac.su.kdt.bepaymentservice.repository.PaymentTransactionRepository;
import ac.su.kdt.bepaymentservice.repository.SubscriptionRepository;
import com.stripe.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StripeWebhookService {
    
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentEventService paymentEventService;
    
    public void handleWebhookEvent(Event event) {
        log.info("Handling Stripe webhook event: {} with ID: {}", event.getType(), event.getId());
        
        switch (event.getType()) {
            case "customer.subscription.created":
                handleSubscriptionCreated(event);
                break;
            case "customer.subscription.updated":
                handleSubscriptionUpdated(event);
                break;
            case "customer.subscription.deleted":
                handleSubscriptionDeleted(event);
                break;
            case "invoice.payment_succeeded":
                handleInvoicePaymentSucceeded(event);
                break;
            case "invoice.payment_failed":
                handleInvoicePaymentFailed(event);
                break;
            case "payment_intent.succeeded":
                handlePaymentIntentSucceeded(event);
                break;
            case "payment_intent.payment_failed":
                handlePaymentIntentFailed(event);
                break;
            case "checkout.session.completed":
                handleCheckoutSessionCompleted(event);
                break;
            default:
                log.info("Unhandled event type: {}", event.getType());
        }
    }
    
    private void handleSubscriptionCreated(Event event) {
        com.stripe.model.Subscription stripeSubscription = (com.stripe.model.Subscription) event.getDataObjectDeserializer().getObject().orElse(null);
        if (stripeSubscription == null) {
            log.error("Failed to deserialize subscription from event");
            return;
        }
        
        log.info("Stripe subscription created: {}", stripeSubscription.getId());
        
        // Find our subscription by customer metadata or create new one
        Long userId = extractUserIdFromMetadata(stripeSubscription.getMetadata());
        if (userId != null) {
            updateSubscriptionFromStripe(stripeSubscription, userId);
        }
    }
    
    private void handleSubscriptionUpdated(Event event) {
        com.stripe.model.Subscription stripeSubscription = (com.stripe.model.Subscription) event.getDataObjectDeserializer().getObject().orElse(null);
        if (stripeSubscription == null) {
            log.error("Failed to deserialize subscription from event");
            return;
        }
        
        log.info("Stripe subscription updated: {}", stripeSubscription.getId());
        
        Optional<Subscription> subscriptionOpt = subscriptionRepository.findByStripeSubscriptionId(stripeSubscription.getId());
        if (subscriptionOpt.isPresent()) {
            Subscription subscription = subscriptionOpt.get();
            updateSubscriptionFromStripe(stripeSubscription, subscription.getUserId());
        }
    }
    
    private void handleSubscriptionDeleted(Event event) {
        com.stripe.model.Subscription stripeSubscription = (com.stripe.model.Subscription) event.getDataObjectDeserializer().getObject().orElse(null);
        if (stripeSubscription == null) {
            log.error("Failed to deserialize subscription from event");
            return;
        }
        
        log.info("Stripe subscription deleted: {}", stripeSubscription.getId());
        
        Optional<Subscription> subscriptionOpt = subscriptionRepository.findByStripeSubscriptionId(stripeSubscription.getId());
        if (subscriptionOpt.isPresent()) {
            Subscription subscription = subscriptionOpt.get();
            subscription.setStatus(Subscription.SubscriptionStatus.CANCELED);
            subscription.setCanceledAt(LocalDateTime.now());
            subscriptionRepository.save(subscription);
            
            paymentEventService.publishSubscriptionCancelled(subscription);
        }
    }
    
    private void handleInvoicePaymentSucceeded(Event event) {
        Invoice invoice = (Invoice) event.getDataObjectDeserializer().getObject().orElse(null);
        if (invoice == null) {
            log.error("Failed to deserialize invoice from event");
            return;
        }
        
        log.info("Invoice payment succeeded: {}", invoice.getId());
        
        if (invoice.getSubscription() != null) {
            Optional<Subscription> subscriptionOpt = subscriptionRepository.findByStripeSubscriptionId(invoice.getSubscription());
            if (subscriptionOpt.isPresent()) {
                Subscription subscription = subscriptionOpt.get();
                
                // Create payment transaction record
                PaymentTransaction transaction = PaymentTransaction.builder()
                    .subscription(subscription)
                    .amount(BigDecimal.valueOf(invoice.getAmountPaid()).divide(BigDecimal.valueOf(100))) // Convert from cents
                    .currency(invoice.getCurrency().toUpperCase())
                    .paymentMethod(PaymentTransaction.PaymentMethod.CARD) // Default to card
                    .transactionStatus(PaymentTransaction.TransactionStatus.SUCCEEDED)
                    .transactionType(PaymentTransaction.TransactionType.SUBSCRIPTION_PAYMENT)
                    .stripeInvoiceId(invoice.getId())
                    .stripeChargeId(invoice.getCharge())
                    .description("Subscription payment")
                    .processedAt(LocalDateTime.ofInstant(Instant.ofEpochSecond(invoice.getStatusTransitions().getPaidAt()), ZoneId.systemDefault()))
                    .build();
                
                paymentTransactionRepository.save(transaction);
                paymentEventService.publishPaymentSucceeded(transaction);
                
                // Update subscription status if needed
                if (subscription.getStatus() != Subscription.SubscriptionStatus.ACTIVE) {
                    subscription.setStatus(Subscription.SubscriptionStatus.ACTIVE);
                    subscriptionRepository.save(subscription);
                    paymentEventService.publishSubscriptionStatusUpdated(subscription);
                }
            }
        }
    }
    
    private void handleInvoicePaymentFailed(Event event) {
        Invoice invoice = (Invoice) event.getDataObjectDeserializer().getObject().orElse(null);
        if (invoice == null) {
            log.error("Failed to deserialize invoice from event");
            return;
        }
        
        log.info("Invoice payment failed: {}", invoice.getId());
        
        if (invoice.getSubscription() != null) {
            Optional<Subscription> subscriptionOpt = subscriptionRepository.findByStripeSubscriptionId(invoice.getSubscription());
            if (subscriptionOpt.isPresent()) {
                Subscription subscription = subscriptionOpt.get();
                
                // Create failed payment transaction record
                PaymentTransaction transaction = PaymentTransaction.builder()
                    .subscription(subscription)
                    .amount(BigDecimal.valueOf(invoice.getAmountDue()).divide(BigDecimal.valueOf(100))) // Convert from cents
                    .currency(invoice.getCurrency().toUpperCase())
                    .paymentMethod(PaymentTransaction.PaymentMethod.CARD) // Default to card
                    .transactionStatus(PaymentTransaction.TransactionStatus.FAILED)
                    .transactionType(PaymentTransaction.TransactionType.SUBSCRIPTION_PAYMENT)
                    .stripeInvoiceId(invoice.getId())
                    .failureReason("Payment failed")
                    .description("Failed subscription payment")
                    .build();
                
                paymentTransactionRepository.save(transaction);
                paymentEventService.publishPaymentFailed(transaction);
                
                // Update subscription status to past due
                subscription.setStatus(Subscription.SubscriptionStatus.PAST_DUE);
                subscriptionRepository.save(subscription);
                paymentEventService.publishSubscriptionStatusUpdated(subscription);
            }
        }
    }
    
    private void handlePaymentIntentSucceeded(Event event) {
        PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer().getObject().orElse(null);
        if (paymentIntent == null) {
            log.error("Failed to deserialize payment intent from event");
            return;
        }
        
        log.info("Payment intent succeeded: {}", paymentIntent.getId());
        
        // Update existing transaction record if exists
        Optional<PaymentTransaction> transactionOpt = paymentTransactionRepository.findByStripePaymentIntentId(paymentIntent.getId());
        if (transactionOpt.isPresent()) {
            PaymentTransaction transaction = transactionOpt.get();
            transaction.setTransactionStatus(PaymentTransaction.TransactionStatus.SUCCEEDED);
            transaction.setProcessedAt(LocalDateTime.ofInstant(Instant.ofEpochSecond(paymentIntent.getCreated()), ZoneId.systemDefault()));
            paymentTransactionRepository.save(transaction);
            
            paymentEventService.publishPaymentSucceeded(transaction);
        }
    }
    
    private void handlePaymentIntentFailed(Event event) {
        PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer().getObject().orElse(null);
        if (paymentIntent == null) {
            log.error("Failed to deserialize payment intent from event");
            return;
        }
        
        log.info("Payment intent failed: {}", paymentIntent.getId());
        
        // Update existing transaction record if exists
        Optional<PaymentTransaction> transactionOpt = paymentTransactionRepository.findByStripePaymentIntentId(paymentIntent.getId());
        if (transactionOpt.isPresent()) {
            PaymentTransaction transaction = transactionOpt.get();
            transaction.setTransactionStatus(PaymentTransaction.TransactionStatus.FAILED);
            transaction.setFailureReason(paymentIntent.getLastPaymentError() != null ? 
                paymentIntent.getLastPaymentError().getMessage() : "Payment failed");
            paymentTransactionRepository.save(transaction);
            
            paymentEventService.publishPaymentFailed(transaction);
        }
    }
    
    private void handleCheckoutSessionCompleted(Event event) {
        com.stripe.model.checkout.Session session = (com.stripe.model.checkout.Session) event.getDataObjectDeserializer().getObject().orElse(null);
        if (session == null) {
            log.error("Failed to deserialize checkout session from event");
            return;
        }
        
        log.info("Checkout session completed: {}", session.getId());
        
        // Extract user ID from metadata
        Long userId = extractUserIdFromMetadata(session.getMetadata());
        if (userId != null && session.getSubscription() != null) {
            // Link our subscription record with Stripe subscription
            Optional<Subscription> subscriptionOpt = subscriptionRepository.findByUserIdAndStatus(userId, Subscription.SubscriptionStatus.INCOMPLETE);
            if (subscriptionOpt.isPresent()) {
                Subscription subscription = subscriptionOpt.get();
                subscription.setStripeSubscriptionId(session.getSubscription());
                subscription.setStripeCustomerId(session.getCustomer());
                subscription.setStatus(Subscription.SubscriptionStatus.ACTIVE);
                subscriptionRepository.save(subscription);
                
                paymentEventService.publishSubscriptionStatusUpdated(subscription);
            }
        }
    }
    
    private void updateSubscriptionFromStripe(com.stripe.model.Subscription stripeSubscription, Long userId) {
        Optional<Subscription> subscriptionOpt = subscriptionRepository.findByStripeSubscriptionId(stripeSubscription.getId());
        Subscription subscription;
        
        if (subscriptionOpt.isPresent()) {
            subscription = subscriptionOpt.get();
        } else {
            // Create new subscription record if not exists
            log.warn("Subscription not found for Stripe subscription: {}, creating new record", stripeSubscription.getId());
            return; // For now, we don't create subscriptions from Stripe events
        }
        
        // Update subscription details from Stripe
        subscription.setStripeSubscriptionId(stripeSubscription.getId());
        subscription.setStripeCustomerId(stripeSubscription.getCustomer());
        subscription.setStatus(mapStripeStatusToOurStatus(stripeSubscription.getStatus()));
        subscription.setCurrentPeriodStart(LocalDateTime.ofInstant(Instant.ofEpochSecond(stripeSubscription.getCurrentPeriodStart()), ZoneId.systemDefault()));
        subscription.setCurrentPeriodEnd(LocalDateTime.ofInstant(Instant.ofEpochSecond(stripeSubscription.getCurrentPeriodEnd()), ZoneId.systemDefault()));
        subscription.setCancelAtPeriodEnd(stripeSubscription.getCancelAtPeriodEnd());
        
        if (stripeSubscription.getCanceledAt() != null) {
            subscription.setCanceledAt(LocalDateTime.ofInstant(Instant.ofEpochSecond(stripeSubscription.getCanceledAt()), ZoneId.systemDefault()));
        }
        
        subscriptionRepository.save(subscription);
        paymentEventService.publishSubscriptionStatusUpdated(subscription);
    }
    
    private Subscription.SubscriptionStatus mapStripeStatusToOurStatus(String stripeStatus) {
        return switch (stripeStatus) {
            case "active" -> Subscription.SubscriptionStatus.ACTIVE;
            case "canceled" -> Subscription.SubscriptionStatus.CANCELED;
            case "incomplete" -> Subscription.SubscriptionStatus.INCOMPLETE;
            case "incomplete_expired" -> Subscription.SubscriptionStatus.INCOMPLETE_EXPIRED;
            case "past_due" -> Subscription.SubscriptionStatus.PAST_DUE;
            case "trialing" -> Subscription.SubscriptionStatus.TRIAL;
            default -> Subscription.SubscriptionStatus.ACTIVE; // Default fallback
        };
    }
    
    private Long extractUserIdFromMetadata(java.util.Map<String, String> metadata) {
        if (metadata != null && metadata.containsKey("user_id")) {
            try {
                return Long.parseLong(metadata.get("user_id"));
            } catch (NumberFormatException e) {
                log.error("Invalid user_id in metadata: {}", metadata.get("user_id"));
            }
        }
        return null;
    }
}