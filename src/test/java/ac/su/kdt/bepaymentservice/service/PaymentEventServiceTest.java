package ac.su.kdt.bepaymentservice.service;

import ac.su.kdt.bepaymentservice.entity.PaymentTransaction;
import ac.su.kdt.bepaymentservice.entity.Subscription;
import ac.su.kdt.bepaymentservice.entity.SubscriptionPlan;
import ac.su.kdt.bepaymentservice.kafka.PaymentEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.*;

@SpringBootTest
@EmbeddedKafka(
        partitions = 1,
        topics = {"test-payment-events", "test-subscription-events"},
        brokerProperties = {
                "listeners=PLAINTEXT://localhost:9092",
                "port=9092"
        }
)
@TestPropertySource(properties = {
        "kafka.topic.payment-events=test-payment-events",
        "kafka.topic.subscription-events=test-subscription-events"
})
@DisplayName("PaymentEventService Kafka 이벤트 테스트")
class PaymentEventServiceTest {
    
    @Autowired
    private PaymentEventService paymentEventService;
    
    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private Consumer<String, PaymentEvent> consumer;
    
    private SubscriptionPlan testPlan;
    private Subscription testSubscription;
    
    @BeforeEach
    void setUp() {
        // Kafka Consumer 설정
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("test-group", "true", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, PaymentEvent.class.getName());
        
        ConsumerFactory<String, PaymentEvent> consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProps);
        consumer = consumerFactory.createConsumer();
        consumer.subscribe(java.util.List.of("test-payment-events", "test-subscription-events"));
        
        // 테스트 데이터 설정
        testPlan = SubscriptionPlan.builder()
                .id(1L)
                .planName("Economy Class")
                .planType(SubscriptionPlan.PlanType.ECONOMY_CLASS)
                .monthlyPrice(new BigDecimal("29.00"))
                .build();
        
        testSubscription = Subscription.builder()
                .id(1L)
                .userId(1L)
                .teamId(100L)
                .plan(testPlan)
                .status(Subscription.SubscriptionStatus.ACTIVE)
                .billingCycle(Subscription.BillingCycle.MONTHLY)
                .amount(new BigDecimal("29.00"))
                .currency("KRW")
                .build();
    }
    
    @AfterEach
    void tearDown() {
        if (consumer != null) {
            consumer.close();
        }
    }
    
    @Test
    @DisplayName("구독 생성 이벤트를 발행한다")
    void publishSubscriptionCreated_Success() {
        // When
        paymentEventService.publishSubscriptionCreated(testSubscription);
        
        // Then
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            ConsumerRecords<String, PaymentEvent> records = consumer.poll(Duration.ofMillis(1000));
            assertThat(records).isNotEmpty();
            
            ConsumerRecord<String, PaymentEvent> record = records.iterator().next();
            PaymentEvent event = record.value();
            
            assertThat(event.getEventType()).isEqualTo(PaymentEvent.EventType.SUBSCRIPTION_CREATED.name());
            assertThat(event.getUserId()).isEqualTo(1L);
            assertThat(event.getTeamId()).isEqualTo(100L);
            assertThat(event.getData()).containsKey("subscriptionId");
            assertThat(event.getData()).containsKey("planId");
            assertThat(event.getData()).containsKey("planType");
            assertThat(event.getData()).containsKey("billingCycle");
            assertThat(event.getData()).containsKey("amount");
            assertThat(event.getData()).containsKey("currency");
            assertThat(event.getData()).containsKey("status");
            assertThat(event.getEventId()).isNotNull();
            assertThat(event.getTimestamp()).isNotNull();
        });
    }
    
    @Test
    @DisplayName("구독 취소 이벤트를 발행한다")
    void publishSubscriptionCancelled_Success() {
        // Given
        testSubscription.setCancelAtPeriodEnd(true);
        testSubscription.setCanceledAt(LocalDateTime.now());
        
        // When
        paymentEventService.publishSubscriptionCancelled(testSubscription);
        
        // Then
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            ConsumerRecords<String, PaymentEvent> records = consumer.poll(Duration.ofMillis(1000));
            assertThat(records).isNotEmpty();
            
            ConsumerRecord<String, PaymentEvent> record = records.iterator().next();
            PaymentEvent event = record.value();
            
            assertThat(event.getEventType()).isEqualTo(PaymentEvent.EventType.SUBSCRIPTION_CANCELLED.name());
            assertThat(event.getUserId()).isEqualTo(1L);
            assertThat(event.getData()).containsKey("subscriptionId");
            assertThat(event.getData()).containsKey("cancelAtPeriodEnd");
            assertThat(event.getData()).containsKey("canceledAt");
        });
    }
    
    @Test
    @DisplayName("구독 만료 이벤트를 발행한다")
    void publishSubscriptionExpired_Success() {
        // Given
        testSubscription.setCurrentPeriodEnd(LocalDateTime.now().minusDays(1));
        
        // When
        paymentEventService.publishSubscriptionExpired(testSubscription);
        
        // Then
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            ConsumerRecords<String, PaymentEvent> records = consumer.poll(Duration.ofMillis(1000));
            assertThat(records).isNotEmpty();
            
            ConsumerRecord<String, PaymentEvent> record = records.iterator().next();
            PaymentEvent event = record.value();
            
            assertThat(event.getEventType()).isEqualTo(PaymentEvent.EventType.SUBSCRIPTION_EXPIRED.name());
            assertThat(event.getUserId()).isEqualTo(1L);
            assertThat(event.getData()).containsKey("subscriptionId");
            assertThat(event.getData()).containsKey("expiredAt");
        });
    }
    
    @Test
    @DisplayName("구독 만료 예정 이벤트를 발행한다")
    void publishSubscriptionExpiring_Success() {
        // Given
        int daysBeforeExpiry = 7;
        testSubscription.setCurrentPeriodEnd(LocalDateTime.now().plusDays(daysBeforeExpiry));
        
        // When
        paymentEventService.publishSubscriptionExpiring(testSubscription, daysBeforeExpiry);
        
        // Then
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            ConsumerRecords<String, PaymentEvent> records = consumer.poll(Duration.ofMillis(1000));
            assertThat(records).isNotEmpty();
            
            ConsumerRecord<String, PaymentEvent> record = records.iterator().next();
            PaymentEvent event = record.value();
            
            assertThat(event.getEventType()).isEqualTo(PaymentEvent.EventType.SUBSCRIPTION_EXPIRING.name());
            assertThat(event.getUserId()).isEqualTo(1L);
            assertThat(event.getData()).containsKey("subscriptionId");
            assertThat(event.getData()).containsKey("expiresAt");
            assertThat(event.getData()).containsKey("daysBeforeExpiry");
            assertThat(event.getData().get("daysBeforeExpiry")).isEqualTo(daysBeforeExpiry);
        });
    }
    
    @Test
    @DisplayName("결제 성공 이벤트를 발행한다")
    void publishPaymentSucceeded_Success() {
        // Given
        PaymentTransaction transaction = PaymentTransaction.builder()
                .id(1L)
                .subscription(testSubscription)
                .amount(new BigDecimal("29.00"))
                .currency("KRW")
                .paymentMethod(PaymentTransaction.PaymentMethod.CARD)
                .transactionStatus(PaymentTransaction.TransactionStatus.SUCCEEDED)
                .stripePaymentIntentId("pi_test123")
                .build();
        
        // When
        paymentEventService.publishPaymentSucceeded(transaction);
        
        // Then
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            ConsumerRecords<String, PaymentEvent> records = consumer.poll(Duration.ofMillis(1000));
            assertThat(records).isNotEmpty();
            
            ConsumerRecord<String, PaymentEvent> record = records.iterator().next();
            PaymentEvent event = record.value();
            
            assertThat(event.getEventType()).isEqualTo(PaymentEvent.EventType.PAYMENT_SUCCEEDED.name());
            assertThat(event.getUserId()).isEqualTo(1L);
            assertThat(event.getData()).containsKey("transactionId");
            assertThat(event.getData()).containsKey("subscriptionId");
            assertThat(event.getData()).containsKey("amount");
            assertThat(event.getData()).containsKey("currency");
            assertThat(event.getData()).containsKey("paymentMethod");
            assertThat(event.getData()).containsKey("stripePaymentIntentId");
        });
    }
    
    @Test
    @DisplayName("결제 실패 이벤트를 발행한다")
    void publishPaymentFailed_Success() {
        // Given
        PaymentTransaction transaction = PaymentTransaction.builder()
                .id(1L)
                .subscription(testSubscription)
                .amount(new BigDecimal("29.00"))
                .currency("KRW")
                .transactionStatus(PaymentTransaction.TransactionStatus.FAILED)
                .failureReason("Card declined")
                .stripePaymentIntentId("pi_test123")
                .build();
        
        // When
        paymentEventService.publishPaymentFailed(transaction);
        
        // Then
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            ConsumerRecords<String, PaymentEvent> records = consumer.poll(Duration.ofMillis(1000));
            assertThat(records).isNotEmpty();
            
            ConsumerRecord<String, PaymentEvent> record = records.iterator().next();
            PaymentEvent event = record.value();
            
            assertThat(event.getEventType()).isEqualTo(PaymentEvent.EventType.PAYMENT_FAILED.name());
            assertThat(event.getUserId()).isEqualTo(1L);
            assertThat(event.getData()).containsKey("transactionId");
            assertThat(event.getData()).containsKey("subscriptionId");
            assertThat(event.getData()).containsKey("amount");
            assertThat(event.getData()).containsKey("currency");
            assertThat(event.getData()).containsKey("failureReason");
            assertThat(event.getData().get("failureReason")).isEqualTo("Card declined");
        });
    }
    
    @Test
    @DisplayName("티켓 사용 이벤트를 발행한다")
    void publishTicketsUsed_Success() {
        // Given
        Long userId = 1L;
        int ticketsUsed = 2;
        int remainingBalance = 3;
        
        // When
        paymentEventService.publishTicketsUsed(userId, ticketsUsed, remainingBalance);
        
        // Then
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            ConsumerRecords<String, PaymentEvent> records = consumer.poll(Duration.ofMillis(1000));
            assertThat(records).isNotEmpty();
            
            ConsumerRecord<String, PaymentEvent> record = records.iterator().next();
            PaymentEvent event = record.value();
            
            assertThat(event.getEventType()).isEqualTo(PaymentEvent.EventType.TICKETS_USED.name());
            assertThat(event.getUserId()).isEqualTo(userId);
            assertThat(event.getData()).containsKey("ticketsUsed");
            assertThat(event.getData()).containsKey("remainingBalance");
            assertThat(event.getData().get("ticketsUsed")).isEqualTo(ticketsUsed);
            assertThat(event.getData().get("remainingBalance")).isEqualTo(remainingBalance);
        });
    }
    
    @Test
    @DisplayName("티켓 환불 이벤트를 발행한다")
    void publishTicketsRefunded_Success() {
        // Given
        Long userId = 1L;
        int ticketsRefunded = 1;
        int newBalance = 6;
        
        // When
        paymentEventService.publishTicketsRefunded(userId, ticketsRefunded, newBalance);
        
        // Then
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            ConsumerRecords<String, PaymentEvent> records = consumer.poll(Duration.ofMillis(1000));
            assertThat(records).isNotEmpty();
            
            ConsumerRecord<String, PaymentEvent> record = records.iterator().next();
            PaymentEvent event = record.value();
            
            assertThat(event.getEventType()).isEqualTo(PaymentEvent.EventType.TICKETS_REFUNDED.name());
            assertThat(event.getUserId()).isEqualTo(userId);
            assertThat(event.getData()).containsKey("ticketsRefunded");
            assertThat(event.getData()).containsKey("newBalance");
            assertThat(event.getData().get("ticketsRefunded")).isEqualTo(ticketsRefunded);
            assertThat(event.getData().get("newBalance")).isEqualTo(newBalance);
        });
    }
    
    @Test
    @DisplayName("티켓 충전 이벤트를 발행한다")
    void publishTicketsRefilled_Success() {
        // Given
        Long userId = 1L;
        int ticketsAdded = 3;
        int newBalance = 5;
        
        // When
        paymentEventService.publishTicketsRefilled(userId, ticketsAdded, newBalance);
        
        // Then
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            ConsumerRecords<String, PaymentEvent> records = consumer.poll(Duration.ofMillis(1000));
            assertThat(records).isNotEmpty();
            
            ConsumerRecord<String, PaymentEvent> record = records.iterator().next();
            PaymentEvent event = record.value();
            
            assertThat(event.getEventType()).isEqualTo(PaymentEvent.EventType.TICKETS_REFILLED.name());
            assertThat(event.getUserId()).isEqualTo(userId);
            assertThat(event.getData()).containsKey("ticketsAdded");
            assertThat(event.getData()).containsKey("newBalance");
            assertThat(event.getData().get("ticketsAdded")).isEqualTo(ticketsAdded);
            assertThat(event.getData().get("newBalance")).isEqualTo(newBalance);
        });
    }
    
    @Test
    @DisplayName("티켓 잔액 부족 이벤트를 발행한다")
    void publishTicketBalanceLow_Success() {
        // Given
        Long userId = 1L;
        int currentBalance = 1;
        int threshold = 2;
        
        // When
        paymentEventService.publishTicketBalanceLow(userId, currentBalance, threshold);
        
        // Then
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            ConsumerRecords<String, PaymentEvent> records = consumer.poll(Duration.ofMillis(1000));
            assertThat(records).isNotEmpty();
            
            ConsumerRecord<String, PaymentEvent> record = records.iterator().next();
            PaymentEvent event = record.value();
            
            assertThat(event.getEventType()).isEqualTo(PaymentEvent.EventType.TICKET_BALANCE_LOW.name());
            assertThat(event.getUserId()).isEqualTo(userId);
            assertThat(event.getData()).containsKey("currentBalance");
            assertThat(event.getData()).containsKey("threshold");
            assertThat(event.getData().get("currentBalance")).isEqualTo(currentBalance);
            assertThat(event.getData().get("threshold")).isEqualTo(threshold);
        });
    }
}