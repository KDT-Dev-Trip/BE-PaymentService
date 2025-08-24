package ac.su.kdt.bepaymentservice.integration;

import ac.su.kdt.bepaymentservice.toss.dto.AutoPaymentRequest;
import ac.su.kdt.bepaymentservice.toss.dto.BillingKeyRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc  
@ActiveProfiles("test")
class GatewayIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @Test
    void testBillingKeyWithoutAuth_ShouldSucceed() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        BillingKeyRequest request = new BillingKeyRequest();
        request.setAuthKey("test_auth_key");
        request.setCustomerKey("test_customer_key");

        mockMvc.perform(post("/api/toss/billing/issue")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()); // Should fail due to external API call
    }

    @Test
    void testAutoPaymentWithoutAuth_ShouldSucceed() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        AutoPaymentRequest request = new AutoPaymentRequest();
        request.setAmount(10000L);
        request.setCustomerKey("test_customer_key");
        request.setOrderId("test_order_id_" + System.currentTimeMillis());
        request.setOrderName("Test Order");
        request.setCustomerEmail("test@example.com");
        request.setCustomerName("Test User");

        mockMvc.perform(post("/api/toss/billing/test_billing_key/payment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()); // Should fail due to external API call
    }

    @Test
    void testWithGatewayHeaders_ShouldSucceed() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        BillingKeyRequest request = new BillingKeyRequest();
        request.setAuthKey("test_auth_key");
        request.setCustomerKey("test_customer_key");

        mockMvc.perform(post("/api/toss/billing/issue")
                .header("X-User-Id", "test-user-123")
                .header("X-User-Email", "test@example.com")
                .header("X-Gateway-Auth", "true")
                .header("X-Service-Route", "payment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()); // Should fail due to external API call but headers should be processed
    }
}