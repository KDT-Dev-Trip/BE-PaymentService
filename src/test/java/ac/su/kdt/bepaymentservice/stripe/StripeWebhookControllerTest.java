package ac.su.kdt.bepaymentservice.stripe;

import ac.su.kdt.bepaymentservice.service.StripeWebhookService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StripeWebhookController.class)
@TestPropertySource(properties = {
        "stripe.webhook.secret=whsec_test_secret"
})
@DisplayName("StripeWebhookController 통합 테스트")
class StripeWebhookControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private StripeWebhookService stripeWebhookService;
    
    private String validPayload;
    private String validSignature;
    private Event mockEvent;
    
    @BeforeEach
    void setUp() {
        validPayload = """
                {
                    "id": "evt_test_webhook",
                    "object": "event",
                    "api_version": "2020-08-27",
                    "created": 1609459200,
                    "data": {
                        "object": {
                            "id": "sub_test123",
                            "object": "subscription",
                            "status": "active"
                        }
                    },
                    "livemode": false,
                    "pending_webhooks": 1,
                    "request": {
                        "id": "req_test123",
                        "idempotency_key": null
                    },
                    "type": "customer.subscription.created"
                }
                """;
        
        validSignature = "t=1609459200,v1=test_signature";
        
        mockEvent = mock(Event.class);
        when(mockEvent.getId()).thenReturn("evt_test_webhook");
        when(mockEvent.getType()).thenReturn("customer.subscription.created");
    }
    
    @Test
    @DisplayName("유효한 웹훅 요청을 성공적으로 처리한다")
    void handleStripeWebhook_ValidRequest_Success() throws Exception {
        // Given
        try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
            webhookMock.when(() -> Webhook.constructEvent(eq(validPayload), eq(validSignature), eq("whsec_test_secret")))
                    .thenReturn(mockEvent);
            
            doNothing().when(stripeWebhookService).handleWebhookEvent(mockEvent);
            
            // When & Then
            mockMvc.perform(post("/api/v1/webhooks/stripe")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validPayload)
                            .header("Stripe-Signature", validSignature))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Webhook processed successfully"));
            
            verify(stripeWebhookService).handleWebhookEvent(mockEvent);
        }
    }
    
    @Test
    @DisplayName("잘못된 서명으로 웹훅 요청 시 400 에러를 반환한다")
    void handleStripeWebhook_InvalidSignature_Returns400() throws Exception {
        // Given
        String invalidSignature = "t=1609459200,v1=invalid_signature";
        
        try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
            webhookMock.when(() -> Webhook.constructEvent(eq(validPayload), eq(invalidSignature), eq("whsec_test_secret")))
                    .thenThrow(new SignatureVerificationException("Invalid signature", invalidSignature));
            
            // When & Then
            mockMvc.perform(post("/api/v1/webhooks/stripe")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validPayload)
                            .header("Stripe-Signature", invalidSignature))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string("Invalid signature"));
            
            verify(stripeWebhookService, never()).handleWebhookEvent(any());
        }
    }
    
    @Test
    @DisplayName("서명 헤더가 없는 웹훅 요청 시 400 에러를 반환한다")
    void handleStripeWebhook_MissingSignatureHeader_Returns400() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/webhooks/stripe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload))
                .andExpect(status().isBadRequest());
        
        verify(stripeWebhookService, never()).handleWebhookEvent(any());
    }
    
    @Test
    @DisplayName("잘못된 JSON 형식의 웹훅 요청 시 400 에러를 반환한다")
    void handleStripeWebhook_InvalidJson_Returns400() throws Exception {
        // Given
        String invalidPayload = "{ invalid json }";
        
        try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
            webhookMock.when(() -> Webhook.constructEvent(eq(invalidPayload), eq(validSignature), eq("whsec_test_secret")))
                    .thenThrow(new RuntimeException("Invalid JSON"));
            
            // When & Then
            mockMvc.perform(post("/api/v1/webhooks/stripe")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidPayload)
                            .header("Stripe-Signature", validSignature))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string("Error parsing webhook"));
            
            verify(stripeWebhookService, never()).handleWebhookEvent(any());
        }
    }
    
    @Test
    @DisplayName("웹훅 처리 중 서비스 에러 발생 시 500 에러를 반환한다")
    void handleStripeWebhook_ServiceError_Returns500() throws Exception {
        // Given
        try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
            webhookMock.when(() -> Webhook.constructEvent(eq(validPayload), eq(validSignature), eq("whsec_test_secret")))
                    .thenReturn(mockEvent);
            
            doThrow(new RuntimeException("Database connection error"))
                    .when(stripeWebhookService).handleWebhookEvent(mockEvent);
            
            // When & Then
            mockMvc.perform(post("/api/v1/webhooks/stripe")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validPayload)
                            .header("Stripe-Signature", validSignature))
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().string("Error processing webhook"));
            
            verify(stripeWebhookService).handleWebhookEvent(mockEvent);
        }
    }
    
    @Test
    @DisplayName("빈 페이로드로 웹훅 요청 시 400 에러를 반환한다")
    void handleStripeWebhook_EmptyPayload_Returns400() throws Exception {
        // Given
        String emptyPayload = "";
        
        try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
            webhookMock.when(() -> Webhook.constructEvent(eq(emptyPayload), eq(validSignature), eq("whsec_test_secret")))
                    .thenThrow(new RuntimeException("Empty payload"));
            
            // When & Then
            mockMvc.perform(post("/api/v1/webhooks/stripe")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(emptyPayload)
                            .header("Stripe-Signature", validSignature))
                    .andExpect(status().isBadRequest());
            
            verify(stripeWebhookService, never()).handleWebhookEvent(any());
        }
    }
    
    @Test
    @DisplayName("긴 페이로드도 정상적으로 처리한다")
    void handleStripeWebhook_LargePayload_Success() throws Exception {
        // Given
        String largePayload = validPayload + "a".repeat(1000); // 큰 페이로드 시뮬레이션
        
        try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
            webhookMock.when(() -> Webhook.constructEvent(eq(largePayload), eq(validSignature), eq("whsec_test_secret")))
                    .thenReturn(mockEvent);
            
            doNothing().when(stripeWebhookService).handleWebhookEvent(mockEvent);
            
            // When & Then
            mockMvc.perform(post("/api/v1/webhooks/stripe")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(largePayload)
                            .header("Stripe-Signature", validSignature))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Webhook processed successfully"));
            
            verify(stripeWebhookService).handleWebhookEvent(mockEvent);
        }
    }
    
    @Test
    @DisplayName("다양한 이벤트 타입을 처리할 수 있다")
    void handleStripeWebhook_DifferentEventTypes_Success() throws Exception {
        // Given
        String invoicePayload = validPayload.replace("customer.subscription.created", "invoice.payment_succeeded");
        Event invoiceEvent = mock(Event.class);
        when(invoiceEvent.getId()).thenReturn("evt_invoice_test");
        when(invoiceEvent.getType()).thenReturn("invoice.payment_succeeded");
        
        try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
            webhookMock.when(() -> Webhook.constructEvent(eq(invoicePayload), eq(validSignature), eq("whsec_test_secret")))
                    .thenReturn(invoiceEvent);
            
            doNothing().when(stripeWebhookService).handleWebhookEvent(invoiceEvent);
            
            // When & Then
            mockMvc.perform(post("/api/v1/webhooks/stripe")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invoicePayload)
                            .header("Stripe-Signature", validSignature))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Webhook processed successfully"));
            
            verify(stripeWebhookService).handleWebhookEvent(invoiceEvent);
        }
    }
    
    @Test
    @DisplayName("Content-Type이 없어도 웹훅을 처리할 수 있다")
    void handleStripeWebhook_NoContentType_Success() throws Exception {
        // Given
        try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
            webhookMock.when(() -> Webhook.constructEvent(eq(validPayload), eq(validSignature), eq("whsec_test_secret")))
                    .thenReturn(mockEvent);
            
            doNothing().when(stripeWebhookService).handleWebhookEvent(mockEvent);
            
            // When & Then
            mockMvc.perform(post("/api/v1/webhooks/stripe")
                            .content(validPayload)
                            .header("Stripe-Signature", validSignature))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Webhook processed successfully"));
            
            verify(stripeWebhookService).handleWebhookEvent(mockEvent);
        }
    }
}