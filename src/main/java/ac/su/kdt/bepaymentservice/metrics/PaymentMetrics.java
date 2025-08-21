package ac.su.kdt.bepaymentservice.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class PaymentMetrics {
    
    private final MeterRegistry meterRegistry;
    
    private final AtomicLong activeSubscriptionsGauge = new AtomicLong(0);
    
    // 구독 관련 메트릭
    private final Counter subscriptionSuccessCounter;
    private final Counter subscriptionFailureCounter;
    private final Timer subscriptionProcessingTime;
    
    // 티켓 관련 메트릭
    private final Counter ticketGrantedCounter;
    private final Counter ticketUsedCounter;
    private final Counter ticketRefundedCounter;
    private final Timer ticketProcessingTime;
    
    // 결제 관련 메트릭
    private final Counter paymentSuccessCounter;
    private final Counter paymentFailureCounter;
    private final Timer paymentProcessingTime;
    
    // Webhook 관련 메트릭
    private final Counter webhookReceivedCounter;
    private final Counter webhookProcessedCounter;
    private final Counter webhookFailureCounter;
    private final Timer webhookProcessingTime;
    
    // Kafka 관련 메트릭
    private final Counter kafkaEventPublishedCounter;
    private final Counter kafkaEventReceivedCounter;
    private final Counter kafkaEventFailureCounter;
    
    public PaymentMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // 구독 메트릭 초기화
        this.subscriptionSuccessCounter = Counter.builder("subscription.success.count")
                .description("Number of successful subscription creations")
                .register(meterRegistry);
        
        this.subscriptionFailureCounter = Counter.builder("subscription.failure.count")
                .description("Number of failed subscription creations")
                .register(meterRegistry);
        
        this.subscriptionProcessingTime = Timer.builder("subscription.processing.time")
                .description("Time taken to process subscription requests")
                .register(meterRegistry);
        
        // 활성 구독 수 게이지
        Gauge.builder("subscription.active.count", activeSubscriptionsGauge, AtomicLong::doubleValue)
                .description("Number of active subscriptions")
                .register(meterRegistry);
        
        // 티켓 메트릭 초기화
        this.ticketGrantedCounter = Counter.builder("ticket.granted.count")
                .description("Number of tickets granted")
                .register(meterRegistry);
        
        this.ticketUsedCounter = Counter.builder("ticket.used.count")
                .description("Number of tickets used")
                .register(meterRegistry);
        
        this.ticketRefundedCounter = Counter.builder("ticket.refunded.count")
                .description("Number of tickets refunded")
                .register(meterRegistry);
        
        this.ticketProcessingTime = Timer.builder("ticket.processing.time")
                .description("Time taken to process ticket operations")
                .register(meterRegistry);
        
        // 결제 메트릭 초기화
        this.paymentSuccessCounter = Counter.builder("payment.success.count")
                .description("Number of successful payments")
                .register(meterRegistry);
        
        this.paymentFailureCounter = Counter.builder("payment.failure.count")
                .description("Number of failed payments")
                .register(meterRegistry);
        
        this.paymentProcessingTime = Timer.builder("payment.processing.time")
                .description("Time taken to process payments")
                .register(meterRegistry);
        
        // Webhook 메트릭 초기화
        this.webhookReceivedCounter = Counter.builder("webhook.received.count")
                .description("Number of webhooks received")
                .register(meterRegistry);
        
        this.webhookProcessedCounter = Counter.builder("webhook.processed.count")
                .description("Number of webhooks processed successfully")
                .register(meterRegistry);
        
        this.webhookFailureCounter = Counter.builder("webhook.failure.count")
                .description("Number of webhook processing failures")
                .register(meterRegistry);
        
        this.webhookProcessingTime = Timer.builder("webhook.processing.time")
                .description("Time taken to process webhooks")
                .register(meterRegistry);
        
        // Kafka 메트릭 초기화
        this.kafkaEventPublishedCounter = Counter.builder("kafka.event.published.count")
                .description("Number of Kafka events published")
                .register(meterRegistry);
        
        this.kafkaEventReceivedCounter = Counter.builder("kafka.event.received.count")
                .description("Number of Kafka events received")
                .register(meterRegistry);
        
        this.kafkaEventFailureCounter = Counter.builder("kafka.event.failure.count")
                .description("Number of Kafka event processing failures")
                .register(meterRegistry);
    }
    
    // 구독 메트릭 메서드
    public void incrementSubscriptionSuccess() {
        subscriptionSuccessCounter.increment();
    }
    
    public void incrementSubscriptionFailure() {
        subscriptionFailureCounter.increment();
    }
    
    public Timer.Sample startSubscriptionTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordSubscriptionProcessingTime(Timer.Sample sample) {
        sample.stop(subscriptionProcessingTime);
    }
    
    public void setActiveSubscriptionsCount(long count) {
        activeSubscriptionsGauge.set(count);
    }
    
    // 티켓 메트릭 메서드
    public void incrementTicketGranted(int amount) {
        ticketGrantedCounter.increment(amount);
    }
    
    public void incrementTicketUsed(int amount) {
        ticketUsedCounter.increment(amount);
    }
    
    public void incrementTicketRefunded(int amount) {
        ticketRefundedCounter.increment(amount);
    }
    
    public Timer.Sample startTicketTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordTicketProcessingTime(Timer.Sample sample) {
        sample.stop(ticketProcessingTime);
    }
    
    // 결제 메트릭 메서드
    public void incrementPaymentSuccess() {
        paymentSuccessCounter.increment();
    }
    
    public void incrementPaymentFailure() {
        paymentFailureCounter.increment();
    }
    
    public Timer.Sample startPaymentTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordPaymentProcessingTime(Timer.Sample sample) {
        sample.stop(paymentProcessingTime);
    }
    
    // Webhook 메트릭 메서드
    public void incrementWebhookReceived() {
        webhookReceivedCounter.increment();
    }
    
    public void incrementWebhookProcessed() {
        webhookProcessedCounter.increment();
    }
    
    public void incrementWebhookFailure() {
        webhookFailureCounter.increment();
    }
    
    public Timer.Sample startWebhookTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordWebhookProcessingTime(Timer.Sample sample) {
        sample.stop(webhookProcessingTime);
    }
    
    // Kafka 메트릭 메서드
    public void incrementKafkaEventPublished(String eventType) {
        kafkaEventPublishedCounter.increment();
    }
    
    public void incrementKafkaEventReceived(String eventType) {
        kafkaEventReceivedCounter.increment();
    }
    
    public void incrementKafkaEventFailure(String eventType) {
        kafkaEventFailureCounter.increment();
    }
    
    // 결제 실패율 계산
    public double getPaymentFailureRate() {
        double totalPayments = paymentSuccessCounter.count() + paymentFailureCounter.count();
        return totalPayments > 0 ? paymentFailureCounter.count() / totalPayments : 0.0;
    }
}