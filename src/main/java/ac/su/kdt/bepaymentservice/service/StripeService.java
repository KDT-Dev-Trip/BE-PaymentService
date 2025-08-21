package ac.su.kdt.bepaymentservice.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Price;
import com.stripe.model.Product;
import com.stripe.model.checkout.Session;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.PriceCreateParams;
import com.stripe.param.ProductCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class StripeService {
    
    @Value("${stripe.api.key}")
    private String stripeApiKey;
    
    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }
    
    public Customer createCustomer(String email, String name, Long userId) throws StripeException {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("user_id", userId.toString());
        
        CustomerCreateParams params = CustomerCreateParams.builder()
                .setEmail(email)
                .setName(name)
                .putAllMetadata(metadata)
                .build();
        
        Customer customer = Customer.create(params);
        log.info("Created Stripe customer: {} for user: {}", customer.getId(), userId);
        return customer;
    }
    
    public Product createProduct(String name, String description) throws StripeException {
        ProductCreateParams params = ProductCreateParams.builder()
                .setName(name)
                .setDescription(description)
                .build();
        
        Product product = Product.create(params);
        log.info("Created Stripe product: {} with name: {}", product.getId(), name);
        return product;
    }
    
    public Price createMonthlyPrice(String productId, BigDecimal amount, String currency) throws StripeException {
        PriceCreateParams params = PriceCreateParams.builder()
                .setProduct(productId)
                .setUnitAmount(amount.multiply(BigDecimal.valueOf(100)).longValue()) // Convert to cents
                .setCurrency(currency.toLowerCase())
                .setRecurring(
                    PriceCreateParams.Recurring.builder()
                        .setInterval(PriceCreateParams.Recurring.Interval.MONTH)
                        .build()
                )
                .build();
        
        Price price = Price.create(params);
        log.info("Created monthly price: {} for product: {} with amount: {} {}", 
                price.getId(), productId, amount, currency);
        return price;
    }
    
    public Price createYearlyPrice(String productId, BigDecimal amount, String currency) throws StripeException {
        PriceCreateParams params = PriceCreateParams.builder()
                .setProduct(productId)
                .setUnitAmount(amount.multiply(BigDecimal.valueOf(100)).longValue()) // Convert to cents
                .setCurrency(currency.toLowerCase())
                .setRecurring(
                    PriceCreateParams.Recurring.builder()
                        .setInterval(PriceCreateParams.Recurring.Interval.YEAR)
                        .build()
                )
                .build();
        
        Price price = Price.create(params);
        log.info("Created yearly price: {} for product: {} with amount: {} {}", 
                price.getId(), productId, amount, currency);
        return price;
    }
    
    public Session createCheckoutSession(String customerId, String priceId, String successUrl, String cancelUrl, Long userId) throws StripeException {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("user_id", userId.toString());
        
        SessionCreateParams params = SessionCreateParams.builder()
                .setCustomer(customerId)
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .addLineItem(
                    SessionCreateParams.LineItem.builder()
                        .setPrice(priceId)
                        .setQuantity(1L)
                        .build()
                )
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .putAllMetadata(metadata)
                .build();
        
        Session session = Session.create(params);
        log.info("Created checkout session: {} for customer: {} with price: {}", 
                session.getId(), customerId, priceId);
        return session;
    }
    
    public Customer retrieveCustomer(String customerId) throws StripeException {
        return Customer.retrieve(customerId);
    }
    
    public Product retrieveProduct(String productId) throws StripeException {
        return Product.retrieve(productId);
    }
    
    public Price retrievePrice(String priceId) throws StripeException {
        return Price.retrieve(priceId);
    }
}