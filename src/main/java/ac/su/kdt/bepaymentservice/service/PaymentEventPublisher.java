package ac.su.kdt.bepaymentservice.service;

import ac.su.kdt.bepaymentservice.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentEventPublisher {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    // Payment Service 전용 토픽명
    private static final String PAYMENT_EVENTS_TOPIC = "payment-events";
    private static final String SUBSCRIPTION_EVENTS_TOPIC = "subscription-events"; // 기존 토픽 호환성
    
    /**
     * 구독 갱신 실패 이벤트 발행
     */
    public void publishSubscriptionRenewalFailedEvent(SubscriptionRenewalFailedEventDTO event) {
        try {
            CompletableFuture<SendResult<String, Object>> future = 
                    kafkaTemplate.send(PAYMENT_EVENTS_TOPIC, event.userId().toString(), event);
            
            future.whenComplete((result, exception) -> {
                if (exception != null) {
                    log.error("Failed to publish SubscriptionRenewalFailedEvent for userId: {}", 
                             event.userId(), exception);
                } else {
                    log.warn("Successfully published SubscriptionRenewalFailedEvent for userId: {}, reason: {}", 
                             event.userId(), event.failureReason());
                }
            });
        } catch (Exception e) {
            log.error("Error publishing SubscriptionRenewalFailedEvent for userId: {}", 
                     event.userId(), e);
        }
    }
    
    /**
     * 구독 변경 이벤트 발행 (기존 subscription-events 토픽도 호환성 유지)
     */
    public void publishSubscriptionChangedEvent(PaymentSubscriptionChangedEventDTO event) {
        try {
            // payment-events 토픽으로 발행
            CompletableFuture<SendResult<String, Object>> paymentFuture = 
                    kafkaTemplate.send(PAYMENT_EVENTS_TOPIC, event.userId().toString(), event);
            
            // 기존 subscription-events 토픽으로도 발행 (호환성)
            CompletableFuture<SendResult<String, Object>> subscriptionFuture = 
                    kafkaTemplate.send(SUBSCRIPTION_EVENTS_TOPIC, event.userId().toString(), event);
            
            paymentFuture.whenComplete((result, exception) -> {
                if (exception != null) {
                    log.error("Failed to publish PaymentSubscriptionChangedEvent to payment-events for userId: {}", 
                             event.userId(), exception);
                } else {
                    log.info("Successfully published PaymentSubscriptionChangedEvent to payment-events for userId: {}, {} -> {}", 
                             event.userId(), event.oldPlan(), event.newPlan());
                }
            });
            
            subscriptionFuture.whenComplete((result, exception) -> {
                if (exception != null) {
                    log.error("Failed to publish PaymentSubscriptionChangedEvent to subscription-events for userId: {}", 
                             event.userId(), exception);
                } else {
                    log.info("Successfully published PaymentSubscriptionChangedEvent to subscription-events for userId: {}", 
                             event.userId());
                }
            });
            
        } catch (Exception e) {
            log.error("Error publishing PaymentSubscriptionChangedEvent for userId: {}", 
                     event.userId(), e);
        }
    }
    
    /**
     * 티켓 잔액 부족 이벤트 발행
     */
    public void publishTicketBalanceLowEvent(TicketBalanceLowEventDTO event) {
        try {
            CompletableFuture<SendResult<String, Object>> future = 
                    kafkaTemplate.send(PAYMENT_EVENTS_TOPIC, event.userId().toString(), event);
            
            future.whenComplete((result, exception) -> {
                if (exception != null) {
                    log.error("Failed to publish TicketBalanceLowEvent for userId: {}", 
                             event.userId(), exception);
                } else {
                    log.warn("Successfully published TicketBalanceLowEvent for userId: {}, balance: {}/{}", 
                             event.userId(), event.currentBalance(), event.thresholdLimit());
                }
            });
        } catch (Exception e) {
            log.error("Error publishing TicketBalanceLowEvent for userId: {}", 
                     event.userId(), e);
        }
    }
    
    // 편의 메서드들
    public void publishRenewalFailed(Long userId, String authUserId, String email, String subscriptionPlan, 
                                   String failureReason, String paymentMethod, Double failedAmount, 
                                   String currency, Integer retryAttemptCount) {
        SubscriptionRenewalFailedEventDTO event = SubscriptionRenewalFailedEventDTO.createDefault(
            userId, authUserId, email, subscriptionPlan, failureReason, 
            paymentMethod, failedAmount, currency, retryAttemptCount
        );
        publishSubscriptionRenewalFailedEvent(event);
    }
    
    public void publishSubscriptionChanged(Long userId, String authUserId, String email, String oldPlan, 
                                         String newPlan, String changeReason, Double oldPrice, Double newPrice, 
                                         String currency, Boolean isProratedRefund, Double proratedAmount) {
        PaymentSubscriptionChangedEventDTO event = PaymentSubscriptionChangedEventDTO.createDefault(
            userId, authUserId, email, oldPlan, newPlan, changeReason, 
            oldPrice, newPrice, currency, isProratedRefund, proratedAmount
        );
        publishSubscriptionChangedEvent(event);
    }
    
    public void publishTicketBalanceLow(Long userId, String authUserId, String email, Integer currentBalance, 
                                      Integer thresholdLimit, String subscriptionPlan, LocalDateTime lastUsedAt, 
                                      Boolean isAutoRechargeEnabled, Integer suggestedRechargeAmount) {
        TicketBalanceLowEventDTO event = TicketBalanceLowEventDTO.createDefault(
            userId, authUserId, email, currentBalance, thresholdLimit, subscriptionPlan, 
            lastUsedAt, isAutoRechargeEnabled, suggestedRechargeAmount
        );
        publishTicketBalanceLowEvent(event);
    }
}