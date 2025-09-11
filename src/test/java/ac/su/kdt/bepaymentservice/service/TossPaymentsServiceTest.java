package ac.su.kdt.bepaymentservice.service;

import ac.su.kdt.bepaymentservice.entity.BillingKey;
import ac.su.kdt.bepaymentservice.repository.BillingKeyRepository;
import ac.su.kdt.bepaymentservice.toss.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class TossPaymentsServiceTest {

    @Mock
    private BillingKeyRepository billingKeyRepository;
    
    @Mock
    private WebClient.Builder webClientBuilder;
    
    @Mock
    private PaymentEventPublisher paymentEventPublisher;
    
    @Mock
    private SubscriptionService subscriptionService;
    
    @InjectMocks
    private TossPaymentsService tossPaymentsService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(tossPaymentsService, "secretKey", "test_secret_key");
        ReflectionTestUtils.setField(tossPaymentsService, "apiUrl", "https://api.tosspayments.com");
    }

    @Test
    void issueBillingKey_ShouldReturnMockResponse() {
        // Given
        BillingKeyRequest request = new BillingKeyRequest();
        request.setCustomerKey("customer_123");
        
        when(billingKeyRepository.existsByCustomerKey(anyString())).thenReturn(false);

        // When
        Mono<BillingResponse> result = tossPaymentsService.issueBillingKey(request);

        // Then
        StepVerifier.create(result)
            .assertNext(response -> {
                assertThat(response).isNotNull();
                assertThat(response.getCustomerKey()).isEqualTo("customer_123");
                assertThat(response.getBillingKey()).startsWith("mock_billing_key_");
                assertThat(response.getMethod()).isEqualTo("카드");
                assertThat(response.getCardCompany()).isEqualTo("테스트카드");
            })
            .verifyComplete();
            
        verify(billingKeyRepository).save(any(BillingKey.class));
    }

    @Test
    void issueBillingKey_ShouldNotSaveDuplicateCustomer() {
        // Given
        BillingKeyRequest request = new BillingKeyRequest();
        request.setCustomerKey("existing_customer");
        
        when(billingKeyRepository.existsByCustomerKey("existing_customer")).thenReturn(true);

        // When
        Mono<BillingResponse> result = tossPaymentsService.issueBillingKey(request);

        // Then
        StepVerifier.create(result)
            .assertNext(response -> {
                assertThat(response).isNotNull();
                assertThat(response.getCustomerKey()).isEqualTo("existing_customer");
            })
            .verifyComplete();
            
        verify(billingKeyRepository, never()).save(any(BillingKey.class));
    }

    @Test
    void processAutoPayment_ShouldReturnMockResponse() {
        // Given
        AutoPaymentRequest request = new AutoPaymentRequest();
        request.setOrderId("subscription-123-1234567890");
        request.setOrderName("구독 결제");
        request.setAmount(10000L);
        
        // When
        Mono<PaymentResponse> result = tossPaymentsService.processAutoPayment("billing_key_123", request);

        // Then
        StepVerifier.create(result)
            .assertNext(response -> {
                assertThat(response).isNotNull();
                assertThat(response.getOrderId()).isEqualTo("subscription-123-1234567890");
                assertThat(response.getOrderName()).isEqualTo("구독 결제");
                assertThat(response.getStatus()).isEqualTo("DONE");
                assertThat(response.getPaymentKey()).startsWith("mock_payment_key_");
            })
            .verifyComplete();
    }

    @Test
    void createCheckout_ShouldReturnMockResponse() {
        // Given
        CheckoutRequest request = new CheckoutRequest();
        request.setOrderId("ticket-456-5000-1234567890");
        request.setOrderName("티켓 구매");
        request.setAmount(5000L);

        // When
        Mono<CheckoutResponse> result = tossPaymentsService.createCheckout(request);

        // Then
        StepVerifier.create(result)
            .assertNext(response -> {
                assertThat(response).isNotNull();
                assertThat(response.getOrderId()).isEqualTo("ticket-456-5000-1234567890");
                assertThat(response.getAmount()).isEqualTo(5000);
                assertThat(response.getCheckoutUrl()).contains("api.tosspayments.com");
                assertThat(response.getPaymentKey()).startsWith("mock_payment_key_");
            })
            .verifyComplete();
    }

    @Test
    void confirmPayment_ShouldReturnMockResponse() {
        // Given
        ConfirmRequest request = new ConfirmRequest();
        request.setPaymentKey("payment_key_123");
        request.setOrderId("subscription-789-1234567890");
        request.setAmount(15000L);

        // When
        Mono<PaymentResponse> result = tossPaymentsService.confirmPayment(request);

        // Then
        StepVerifier.create(result)
            .assertNext(response -> {
                assertThat(response).isNotNull();
                assertThat(response.getPaymentKey()).isEqualTo("payment_key_123");
                assertThat(response.getOrderId()).isEqualTo("subscription-789-1234567890");
                assertThat(response.getOrderName()).isEqualTo("구독 결제");
                assertThat(response.getStatus()).isEqualTo("DONE");
            })
            .verifyComplete();
    }
}