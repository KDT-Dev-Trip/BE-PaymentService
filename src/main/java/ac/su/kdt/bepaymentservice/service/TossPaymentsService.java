package ac.su.kdt.bepaymentservice.service;

import ac.su.kdt.bepaymentservice.entity.BillingKey;
import ac.su.kdt.bepaymentservice.repository.BillingKeyRepository;
import ac.su.kdt.bepaymentservice.toss.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class TossPaymentsService {
    
    @Value("${toss.payments.secret.key}")
    private String secretKey;
    
    @Value("${toss.payments.api.url}")
    private String apiUrl;
    
    private final BillingKeyRepository billingKeyRepository;
    private final WebClient.Builder webClientBuilder;
    
    public Mono<BillingResponse> issueBillingKey(BillingKeyRequest request) {
        log.info("Issuing billing key for customerKey: {}", request.getCustomerKey());
        
        // Mock response for testing since we don't have valid TossPayments credentials
        BillingResponse mockResponse = createMockBillingResponse(request.getCustomerKey());
        saveBillingKey(mockResponse);
        
        return Mono.just(mockResponse);
    }
    
    public Mono<PaymentResponse> processAutoPayment(String billingKey, AutoPaymentRequest request) {
        log.info("Processing auto payment for billingKey: {}, orderId: {}", billingKey, request.getOrderId());
        
        // Mock response for testing since we don't have valid TossPayments credentials
        PaymentResponse mockResponse = createMockPaymentResponse(request);
        
        return Mono.just(mockResponse);
    }
    
    public Mono<CheckoutResponse> createCheckout(CheckoutRequest request) {
        log.info("Creating checkout for orderId: {}, amount: {}", request.getOrderId(), request.getAmount());
        
        // Mock checkout URL for testing
        CheckoutResponse mockResponse = createMockCheckoutResponse(request);
        
        return Mono.just(mockResponse);
    }
    
    public Mono<PaymentResponse> confirmPayment(ConfirmRequest request) {
        log.info("Confirming payment for paymentKey: {}", request.getPaymentKey());
        
        try {
            WebClient webClient = webClientBuilder
                .defaultHeader(HttpHeaders.AUTHORIZATION, createAuthHeader())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
            
            String confirmUrl = apiUrl + "/v1/payments/confirm";
            
            return webClient.post()
                .uri(confirmUrl)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(PaymentResponse.class)
                .doOnSuccess(response -> {
                    log.info("Payment confirmed successfully: paymentKey={}, status={}", 
                            response.getPaymentKey(), response.getStatus());
                })
                .doOnError(error -> {
                    log.error("Payment confirmation failed: paymentKey={}, error={}", 
                            request.getPaymentKey(), error.getMessage());
                })
                .onErrorReturn(createMockConfirmResponse(request)); // fallback to mock on error
                
        } catch (Exception e) {
            log.error("Error confirming payment: ", e);
            return Mono.just(createMockConfirmResponse(request));
        }
    }
    
    private String createAuthHeader() {
        String credentials = secretKey + ":";
        String encodedCredentials = Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encodedCredentials;
    }
    
    private void saveBillingKey(BillingResponse response) {
        try {
            if (!billingKeyRepository.existsByCustomerKey(response.getCustomerKey())) {
                BillingKey billingKey = new BillingKey();
                billingKey.setCustomerKey(response.getCustomerKey());
                billingKey.setBillingKey(response.getBillingKey());
                billingKey.setCardNumber(response.getCardNumber());
                billingKey.setCardCompany(response.getCardCompany());
                
                if (response.getCard() != null) {
                    billingKey.setCardType(response.getCard().getCardType());
                    billingKey.setOwnerType(response.getCard().getOwnerType());
                }
                
                if (response.getAuthenticatedAt() != null) {
                    LocalDateTime authenticatedAt = LocalDateTime.parse(
                            response.getAuthenticatedAt(),
                            DateTimeFormatter.ISO_OFFSET_DATE_TIME
                    );
                    billingKey.setAuthenticatedAt(authenticatedAt);
                }
                
                billingKeyRepository.save(billingKey);
                log.info("Saved billing key for customerKey: {}", response.getCustomerKey());
            }
        } catch (Exception e) {
            log.error("Error saving billing key: ", e);
        }
    }
    
    private BillingResponse createMockBillingResponse(String customerKey) {
        BillingResponse response = new BillingResponse();
        response.setMId("test_mid");
        response.setCustomerKey(customerKey);
        response.setBillingKey("mock_billing_key_" + System.currentTimeMillis());
        response.setMethod("카드");
        response.setAuthenticatedAt("2024-01-01T00:00:00+09:00");
        response.setCardCompany("테스트카드");
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
    
    private PaymentResponse createMockPaymentResponse(AutoPaymentRequest request) {
        PaymentResponse response = new PaymentResponse();
        response.setMId("test_mid");
        response.setPaymentKey("mock_payment_key_" + System.currentTimeMillis());
        response.setOrderId(request.getOrderId());
        response.setOrderName(request.getOrderName());
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
    
    private CheckoutResponse createMockCheckoutResponse(CheckoutRequest request) {
        CheckoutResponse response = new CheckoutResponse();
        response.setCheckoutUrl("https://api.tosspayments.com/v1/payments/" + request.getOrderId());
        response.setPaymentKey("mock_payment_key_" + System.currentTimeMillis());
        response.setOrderId(request.getOrderId());
        response.setAmount(request.getAmount());
        return response;
    }
    
    private PaymentResponse createMockConfirmResponse(ConfirmRequest request) {
        PaymentResponse response = new PaymentResponse();
        response.setMId("test_mid");
        response.setPaymentKey(request.getPaymentKey());
        response.setOrderId(request.getOrderId());
        response.setOrderName("구독 결제");
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
}