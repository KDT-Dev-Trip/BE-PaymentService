package ac.su.kdt.bepaymentservice.service;

import ac.su.kdt.bepaymentservice.entity.BillingKey;
import ac.su.kdt.bepaymentservice.repository.BillingKeyRepository;
import ac.su.kdt.bepaymentservice.toss.dto.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TossPaymentsServiceTest {

    @Mock
    private BillingKeyRepository billingKeyRepository;

    private TossPaymentsService tossPaymentsService;
    private MockWebServer mockWebServer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        
        String baseUrl = mockWebServer.url("/").toString();
        WebClient.Builder webClientBuilder = WebClient.builder();
        
        tossPaymentsService = new TossPaymentsService(
            billingKeyRepository,
            webClientBuilder
        );
        
        // Use reflection to set the required fields
        setField(tossPaymentsService, "secretKey", "test_sk_test_key");
        setField(tossPaymentsService, "apiUrl", baseUrl);
        
        objectMapper = new ObjectMapper();
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void issueBillingKey_Success() throws JsonProcessingException, InterruptedException {
        // Given
        BillingKeyRequest request = new BillingKeyRequest();
        request.setAuthKey("test_auth_key");
        request.setCustomerKey("test_customer_key");

        BillingResponse mockResponse = createMockBillingResponse();
        
        mockWebServer.enqueue(new MockResponse()
            .setBody(objectMapper.writeValueAsString(mockResponse))
            .addHeader("Content-Type", "application/json"));

        when(billingKeyRepository.existsByCustomerKey("test_customer_key")).thenReturn(false);
        when(billingKeyRepository.save(any(BillingKey.class))).thenReturn(new BillingKey());

        // When & Then
        StepVerifier.create(tossPaymentsService.issueBillingKey(request))
            .expectNextMatches(response -> {
                assertThat(response.getCustomerKey()).isEqualTo("test_customer_key");
                assertThat(response.getBillingKey()).isEqualTo("test_billing_key");
                assertThat(response.getMethod()).isEqualTo("카드");
                return true;
            })
            .verifyComplete();

        // Verify the request
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getPath()).isEqualTo("/v1/billing/authorizations/issue");
        assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        assertThat(recordedRequest.getHeader("Authorization")).startsWith("Basic");
    }

    @Test
    void processAutoPayment_Success() throws JsonProcessingException, InterruptedException {
        // Given
        AutoPaymentRequest request = new AutoPaymentRequest();
        request.setAmount(10000L);
        request.setCustomerKey("test_customer_key");
        request.setOrderId("test_order_id");
        request.setOrderName("Test Order");
        request.setCustomerEmail("test@example.com");
        request.setCustomerName("Test User");

        PaymentResponse mockResponse = createMockPaymentResponse();
        
        mockWebServer.enqueue(new MockResponse()
            .setBody(objectMapper.writeValueAsString(mockResponse))
            .addHeader("Content-Type", "application/json"));

        // When & Then
        StepVerifier.create(tossPaymentsService.processAutoPayment("test_billing_key", request))
            .expectNextMatches(response -> {
                assertThat(response.getOrderId()).isEqualTo("test_order_id");
                assertThat(response.getOrderName()).isEqualTo("Test Order");
                assertThat(response.getStatus()).isEqualTo("DONE");
                return true;
            })
            .verifyComplete();

        // Verify the request
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getPath()).isEqualTo("/v1/billing/test_billing_key");
        assertThat(recordedRequest.getMethod()).isEqualTo("POST");
    }

    @Test
    void issueBillingKey_ServerError() {
        // Given
        BillingKeyRequest request = new BillingKeyRequest();
        request.setAuthKey("test_auth_key");
        request.setCustomerKey("test_customer_key");

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(500)
            .setBody("{\"error\":\"Internal Server Error\"}"));

        // When & Then
        StepVerifier.create(tossPaymentsService.issueBillingKey(request))
            .expectError()
            .verify();
    }

    private BillingResponse createMockBillingResponse() {
        BillingResponse response = new BillingResponse();
        response.setMId("test_mid");
        response.setCustomerKey("test_customer_key");
        response.setBillingKey("test_billing_key");
        response.setMethod("카드");
        response.setAuthenticatedAt("2024-01-01T00:00:00+09:00");
        response.setCardCompany("현대");
        response.setCardNumber("1234****5678");

        BillingResponse.CardInfo cardInfo = new BillingResponse.CardInfo();
        cardInfo.setIssuerCode("61");
        cardInfo.setAcquirerCode("31");
        cardInfo.setNumber("1234****5678");
        cardInfo.setCardType("신용");
        cardInfo.setOwnerType("개인");
        
        response.setCard(cardInfo);
        return response;
    }

    private PaymentResponse createMockPaymentResponse() {
        PaymentResponse response = new PaymentResponse();
        response.setMId("test_mid");
        response.setPaymentKey("test_payment_key");
        response.setOrderId("test_order_id");
        response.setOrderName("Test Order");
        response.setStatus("DONE");
        response.setRequestedAt("2024-01-01T00:00:00+09:00");
        response.setApprovedAt("2024-01-01T00:01:00+09:00");

        PaymentResponse.CardInfo cardInfo = new PaymentResponse.CardInfo();
        cardInfo.setIssuerCode("61");
        cardInfo.setAcquirerCode("31");
        cardInfo.setNumber("1234****5678");
        cardInfo.setCardType("신용");
        cardInfo.setOwnerType("개인");
        
        response.setCard(cardInfo);
        return response;
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}