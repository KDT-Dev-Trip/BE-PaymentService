package ac.su.kdt.bepaymentservice.service;

import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Price;
import com.stripe.model.Product;
import com.stripe.model.checkout.Session;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.PriceCreateParams;
import com.stripe.param.ProductCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StripeService 단위 테스트")
class StripeServiceTest {
    
    @InjectMocks
    private StripeService stripeService;
    
    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(stripeService, "stripeApiKey", "sk_test_fake_key");
        stripeService.init();
    }
    
    @Test
    @DisplayName("Stripe 고객을 성공적으로 생성한다")
    void createCustomer_Success() throws StripeException {
        // Given
        String email = "test@example.com";
        String name = "Test User";
        Long userId = 1L;
        
        Customer mockCustomer = mock(Customer.class);
        when(mockCustomer.getId()).thenReturn("cus_test123");
        when(mockCustomer.getEmail()).thenReturn(email);
        when(mockCustomer.getName()).thenReturn(name);
        
        try (MockedStatic<Customer> customerMock = mockStatic(Customer.class)) {
            customerMock.when(() -> Customer.create(any(CustomerCreateParams.class))).thenReturn(mockCustomer);
            
            // When
            Customer result = stripeService.createCustomer(email, name, userId);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo("cus_test123");
            assertThat(result.getEmail()).isEqualTo(email);
            assertThat(result.getName()).isEqualTo(name);
            
            customerMock.verify(() -> Customer.create(any(CustomerCreateParams.class)));
        }
    }
    
    @Test
    @DisplayName("Stripe 고객 생성 실패 시 예외를 전파한다")
    void createCustomer_StripeException_PropagatesException() {
        // Given
        String email = "test@example.com";
        String name = "Test User";
        Long userId = 1L;
        
        try (MockedStatic<Customer> customerMock = mockStatic(Customer.class)) {
            customerMock.when(() -> Customer.create(any(CustomerCreateParams.class)))
                    .thenThrow(new StripeException("Card declined", "card_declined", "decline_code", 402) {});
            
            // When & Then
            assertThatThrownBy(() -> stripeService.createCustomer(email, name, userId))
                    .isInstanceOf(StripeException.class)
                    .hasMessageContaining("Card declined");
        }
    }
    
    @Test
    @DisplayName("Stripe 제품을 성공적으로 생성한다")
    void createProduct_Success() throws StripeException {
        // Given
        String name = "Test Product";
        String description = "Test Description";
        
        Product mockProduct = mock(Product.class);
        when(mockProduct.getId()).thenReturn("prod_test123");
        when(mockProduct.getName()).thenReturn(name);
        when(mockProduct.getDescription()).thenReturn(description);
        
        try (MockedStatic<Product> productMock = mockStatic(Product.class)) {
            productMock.when(() -> Product.create(any(ProductCreateParams.class))).thenReturn(mockProduct);
            
            // When
            Product result = stripeService.createProduct(name, description);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo("prod_test123");
            assertThat(result.getName()).isEqualTo(name);
            assertThat(result.getDescription()).isEqualTo(description);
            
            productMock.verify(() -> Product.create(any(ProductCreateParams.class)));
        }
    }
    
    @Test
    @DisplayName("월간 가격을 성공적으로 생성한다")
    void createMonthlyPrice_Success() throws StripeException {
        // Given
        String productId = "prod_test123";
        BigDecimal amount = new BigDecimal("29.00");
        String currency = "KRW";
        
        Price mockPrice = mock(Price.class);
        when(mockPrice.getId()).thenReturn("price_test123");
        when(mockPrice.getProduct()).thenReturn(productId);
        when(mockPrice.getUnitAmount()).thenReturn(2900L); // 29.00 * 100
        
        try (MockedStatic<Price> priceMock = mockStatic(Price.class)) {
            priceMock.when(() -> Price.create(any(PriceCreateParams.class))).thenReturn(mockPrice);
            
            // When
            Price result = stripeService.createMonthlyPrice(productId, amount, currency);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo("price_test123");
            assertThat(result.getProduct()).isEqualTo(productId);
            assertThat(result.getUnitAmount()).isEqualTo(2900L);
            
            priceMock.verify(() -> Price.create(any(PriceCreateParams.class)));
        }
    }
    
    @Test
    @DisplayName("연간 가격을 성공적으로 생성한다")
    void createYearlyPrice_Success() throws StripeException {
        // Given
        String productId = "prod_test123";
        BigDecimal amount = new BigDecimal("290.00");
        String currency = "KRW";
        
        Price mockPrice = mock(Price.class);
        when(mockPrice.getId()).thenReturn("price_yearly_test123");
        when(mockPrice.getProduct()).thenReturn(productId);
        when(mockPrice.getUnitAmount()).thenReturn(29000L); // 290.00 * 100
        
        try (MockedStatic<Price> priceMock = mockStatic(Price.class)) {
            priceMock.when(() -> Price.create(any(PriceCreateParams.class))).thenReturn(mockPrice);
            
            // When
            Price result = stripeService.createYearlyPrice(productId, amount, currency);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo("price_yearly_test123");
            assertThat(result.getProduct()).isEqualTo(productId);
            assertThat(result.getUnitAmount()).isEqualTo(29000L);
            
            priceMock.verify(() -> Price.create(any(PriceCreateParams.class)));
        }
    }
    
    @Test
    @DisplayName("체크아웃 세션을 성공적으로 생성한다")
    void createCheckoutSession_Success() throws StripeException {
        // Given
        String customerId = "cus_test123";
        String priceId = "price_test123";
        String successUrl = "https://example.com/success";
        String cancelUrl = "https://example.com/cancel";
        Long userId = 1L;
        
        Session mockSession = mock(Session.class);
        when(mockSession.getId()).thenReturn("cs_test123");
        when(mockSession.getUrl()).thenReturn("https://checkout.stripe.com/pay/cs_test123");
        when(mockSession.getCustomer()).thenReturn(customerId);
        
        try (MockedStatic<Session> sessionMock = mockStatic(Session.class)) {
            sessionMock.when(() -> Session.create(any(SessionCreateParams.class))).thenReturn(mockSession);
            
            // When
            Session result = stripeService.createCheckoutSession(customerId, priceId, successUrl, cancelUrl, userId);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo("cs_test123");
            assertThat(result.getUrl()).isEqualTo("https://checkout.stripe.com/pay/cs_test123");
            assertThat(result.getCustomer()).isEqualTo(customerId);
            
            sessionMock.verify(() -> Session.create(any(SessionCreateParams.class)));
        }
    }
    
    @Test
    @DisplayName("Stripe 고객을 성공적으로 조회한다")
    void retrieveCustomer_Success() throws StripeException {
        // Given
        String customerId = "cus_test123";
        
        Customer mockCustomer = mock(Customer.class);
        when(mockCustomer.getId()).thenReturn(customerId);
        when(mockCustomer.getEmail()).thenReturn("test@example.com");
        
        try (MockedStatic<Customer> customerMock = mockStatic(Customer.class)) {
            customerMock.when(() -> Customer.retrieve(customerId)).thenReturn(mockCustomer);
            
            // When
            Customer result = stripeService.retrieveCustomer(customerId);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(customerId);
            assertThat(result.getEmail()).isEqualTo("test@example.com");
            
            customerMock.verify(() -> Customer.retrieve(customerId));
        }
    }
    
    @Test
    @DisplayName("존재하지 않는 고객 조회 시 예외를 전파한다")
    void retrieveCustomer_NotFound_PropagatesException() {
        // Given
        String customerId = "cus_nonexistent";
        
        try (MockedStatic<Customer> customerMock = mockStatic(Customer.class)) {
            customerMock.when(() -> Customer.retrieve(customerId))
                    .thenThrow(new StripeException("No such customer", "resource_missing", null, 404) {});
            
            // When & Then
            assertThatThrownBy(() -> stripeService.retrieveCustomer(customerId))
                    .isInstanceOf(StripeException.class)
                    .hasMessageContaining("No such customer");
        }
    }
    
    @Test
    @DisplayName("Stripe 제품을 성공적으로 조회한다")
    void retrieveProduct_Success() throws StripeException {
        // Given
        String productId = "prod_test123";
        
        Product mockProduct = mock(Product.class);
        when(mockProduct.getId()).thenReturn(productId);
        when(mockProduct.getName()).thenReturn("Test Product");
        
        try (MockedStatic<Product> productMock = mockStatic(Product.class)) {
            productMock.when(() -> Product.retrieve(productId)).thenReturn(mockProduct);
            
            // When
            Product result = stripeService.retrieveProduct(productId);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(productId);
            assertThat(result.getName()).isEqualTo("Test Product");
            
            productMock.verify(() -> Product.retrieve(productId));
        }
    }
    
    @Test
    @DisplayName("Stripe 가격을 성공적으로 조회한다")
    void retrievePrice_Success() throws StripeException {
        // Given
        String priceId = "price_test123";
        
        Price mockPrice = mock(Price.class);
        when(mockPrice.getId()).thenReturn(priceId);
        when(mockPrice.getUnitAmount()).thenReturn(2900L);
        
        try (MockedStatic<Price> priceMock = mockStatic(Price.class)) {
            priceMock.when(() -> Price.retrieve(priceId)).thenReturn(mockPrice);
            
            // When
            Price result = stripeService.retrievePrice(priceId);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(priceId);
            assertThat(result.getUnitAmount()).isEqualTo(2900L);
            
            priceMock.verify(() -> Price.retrieve(priceId));
        }
    }
}