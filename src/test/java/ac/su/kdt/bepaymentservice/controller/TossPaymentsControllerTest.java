package ac.su.kdt.bepaymentservice.controller;

import ac.su.kdt.bepaymentservice.service.TossPaymentsService;
import ac.su.kdt.bepaymentservice.toss.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TossPaymentsController.class)
class TossPaymentsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TossPaymentsService tossPaymentsService;

    @Test
    void issueBillingKey_Success() throws Exception {
        // Given
        BillingKeyRequest request = new BillingKeyRequest();
        request.setAuthKey("test_auth_key");
        request.setCustomerKey("test_customer_key");

        BillingResponse response = createMockBillingResponse();
        
        when(tossPaymentsService.issueBillingKey(any(BillingKeyRequest.class)))
            .thenReturn(Mono.just(response));

        // When & Then
        mockMvc.perform(post("/api/toss/billing/issue")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerKey").value("test_customer_key"))
                .andExpect(jsonPath("$.billingKey").value("test_billing_key"))
                .andExpect(jsonPath("$.method").value("카드"));
    }

    @Test
    void issueBillingKey_Error() throws Exception {
        // Given
        BillingKeyRequest request = new BillingKeyRequest();
        request.setAuthKey("test_auth_key");
        request.setCustomerKey("test_customer_key");

        when(tossPaymentsService.issueBillingKey(any(BillingKeyRequest.class)))
            .thenReturn(Mono.error(new RuntimeException("API Error")));

        // When & Then
        mockMvc.perform(post("/api/toss/billing/issue")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void processAutoPayment_Success() throws Exception {
        // Given
        AutoPaymentRequest request = new AutoPaymentRequest();
        request.setAmount(10000L);
        request.setCustomerKey("test_customer_key");
        request.setOrderId("test_order_id");
        request.setOrderName("Test Order");
        request.setCustomerEmail("test@example.com");
        request.setCustomerName("Test User");

        PaymentResponse response = createMockPaymentResponse();
        
        when(tossPaymentsService.processAutoPayment(eq("test_billing_key"), any(AutoPaymentRequest.class)))
            .thenReturn(Mono.just(response));

        // When & Then
        mockMvc.perform(post("/api/toss/billing/test_billing_key/payment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("test_order_id"))
                .andExpect(jsonPath("$.orderName").value("Test Order"))
                .andExpect(jsonPath("$.status").value("DONE"));
    }

    @Test
    void processAutoPayment_Error() throws Exception {
        // Given
        AutoPaymentRequest request = new AutoPaymentRequest();
        request.setAmount(10000L);
        request.setCustomerKey("test_customer_key");
        request.setOrderId("test_order_id");
        request.setOrderName("Test Order");

        when(tossPaymentsService.processAutoPayment(eq("test_billing_key"), any(AutoPaymentRequest.class)))
            .thenReturn(Mono.error(new RuntimeException("Payment Error")));

        // When & Then
        mockMvc.perform(post("/api/toss/billing/test_billing_key/payment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
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
}