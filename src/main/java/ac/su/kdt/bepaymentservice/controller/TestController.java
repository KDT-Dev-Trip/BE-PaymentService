package ac.su.kdt.bepaymentservice.controller;

import ac.su.kdt.bepaymentservice.entity.SubscriptionPlan;
import ac.su.kdt.bepaymentservice.repository.SubscriptionPlanRepository;
import ac.su.kdt.bepaymentservice.service.StripeService;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Price;
import com.stripe.model.Product;
import com.stripe.model.checkout.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/test")
@RequiredArgsConstructor
@Slf4j
public class TestController {
    
    private final StripeService stripeService;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    
    @GetMapping("/plans")
    public ResponseEntity<List<SubscriptionPlan>> getPlans() {
        List<SubscriptionPlan> plans = subscriptionPlanRepository.findAll();
        return ResponseEntity.ok(plans);
    }
    
    @GetMapping("/checkout-page")
    public ResponseEntity<String> getCheckoutPage() {
        String html = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Stripe 결제 테스트</title>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; max-width: 600px; margin: 50px auto; padding: 20px; }
                    .button { 
                        background: #635bff; color: white; padding: 12px 24px; 
                        border: none; border-radius: 6px; font-size: 16px; 
                        cursor: pointer; text-decoration: none; display: inline-block;
                        margin: 10px 0;
                    }
                    .button:hover { background: #5a52e5; }
                    .result { margin-top: 20px; padding: 15px; background: #f8f9fa; border-radius: 6px; }
                </style>
            </head>
            <body>
                <h1>🚀 Stripe 결제 테스트</h1>
                <p>아래 버튼을 클릭하면 Stripe 체크아웃 세션이 생성되고 결제 화면으로 이동합니다.</p>
                
                <button class="button" onclick="createCheckout()">Stripe 결제 화면 생성</button>
                
                <div id="result" class="result" style="display:none;">
                    <h3>결제 링크가 생성되었습니다!</h3>
                    <p>아래 링크를 클릭하여 실제 Stripe 결제 화면으로 이동하세요:</p>
                    <a id="checkout-link" class="button" target="_blank">Stripe 결제 화면 열기</a>
                </div>
                
                <script>
                async function createCheckout() {
                    try {
                        const response = await fetch('/api/v1/test/stripe-checkout-simple');
                        const data = await response.json();
                        
                        if (data.checkoutUrl) {
                            document.getElementById('checkout-link').href = data.checkoutUrl;
                            document.getElementById('result').style.display = 'block';
                            
                            // 자동으로 새 탭에서 열기
                            window.open(data.checkoutUrl, '_blank');
                        } else {
                            alert('오류: ' + (data.error || '알 수 없는 오류'));
                        }
                    } catch (error) {
                        alert('요청 실패: ' + error.message);
                    }
                }
                </script>
                
                <hr style="margin: 40px 0;">
                <h3>💳 테스트 카드 정보</h3>
                <ul>
                    <li><strong>카드 번호:</strong> 4242 4242 4242 4242</li>
                    <li><strong>만료일:</strong> 12/34</li>
                    <li><strong>CVC:</strong> 123</li>
                    <li><strong>이름:</strong> 아무 이름</li>
                    <li><strong>이메일:</strong> 유효한 이메일</li>
                </ul>
            </body>
            </html>
            """;
        
        return ResponseEntity.ok()
            .header("Content-Type", "text/html; charset=UTF-8")
            .body(html);
    }
    
    @PostMapping("/stripe-checkout")
    public ResponseEntity<Map<String, String>> createTestCheckout() {
        try {
            // 1. 테스트 고객 생성
            Customer customer = stripeService.createCustomer(
                "test@example.com", 
                "Test User", 
                12345L
            );
            log.info("Created test customer: {}", customer.getId());
            
            // 2. 테스트 제품 생성
            Product product = stripeService.createProduct(
                "Test Subscription", 
                "Test subscription for payment testing"
            );
            log.info("Created test product: {}", product.getId());
            
            // 3. 월간 가격 생성 (29.00 USD)
            Price price = stripeService.createMonthlyPrice(
                product.getId(), 
                java.math.BigDecimal.valueOf(29.00), 
                "USD"
            );
            log.info("Created test price: {}", price.getId());
            
            // 4. 체크아웃 세션 생성
            Session session = stripeService.createCheckoutSession(
                customer.getId(),
                price.getId(),
                "https://example.com/success?session_id={CHECKOUT_SESSION_ID}",
                "https://example.com/cancel",
                12345L
            );
            
            log.info("Created checkout session: {}", session.getId());
            log.info("Checkout URL: {}", session.getUrl());
            
            return ResponseEntity.ok(Map.of(
                "checkoutUrl", session.getUrl(),
                "sessionId", session.getId(),
                "customerId", customer.getId(),
                "productId", product.getId(),
                "priceId", price.getId()
            ));
            
        } catch (StripeException e) {
            log.error("Stripe error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Stripe error: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Unexpected error: " + e.getMessage()));
        }
    }
    
    @GetMapping("/stripe-checkout-simple")
    public ResponseEntity<Map<String, String>> createSimpleCheckout(
            @RequestParam(defaultValue = "29.00") String amount,
            @RequestParam(defaultValue = "USD") String currency) {
        try {
            log.info("Creating simple checkout with amount: {} {}", amount, currency);
            
            // 간단한 제품과 가격을 바로 생성
            Product product = stripeService.createProduct(
                "Simple Test Product", 
                "Simple test product for " + amount + " " + currency
            );
            
            Price price = stripeService.createMonthlyPrice(
                product.getId(), 
                new java.math.BigDecimal(amount), 
                currency
            );
            
            Customer customer = stripeService.createCustomer(
                "simple-test@example.com", 
                "Simple Test User", 
                System.currentTimeMillis()
            );
            
            Session session = stripeService.createCheckoutSession(
                customer.getId(),
                price.getId(),
                "http://localhost:8081/api/v1/test/payment-success?session_id={CHECKOUT_SESSION_ID}",
                "http://localhost:8081/api/v1/test/payment-cancel",
                System.currentTimeMillis()
            );
            
            log.info("Simple checkout created successfully");
            log.info("Checkout URL: {}", session.getUrl());
            
            return ResponseEntity.ok(Map.of(
                "checkoutUrl", session.getUrl(),
                "message", "Checkout session created successfully"
            ));
            
        } catch (Exception e) {
            log.error("Error creating simple checkout: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/payment-success")
    public ResponseEntity<String> paymentSuccess(@RequestParam String session_id) {
        log.info("Payment successful! Session ID: {}", session_id);
        
        String html = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>결제 완료!</title>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; max-width: 600px; margin: 50px auto; padding: 20px; text-align: center; }
                    .success { color: #28a745; font-size: 24px; margin-bottom: 20px; }
                    .session-id { background: #f8f9fa; padding: 10px; border-radius: 5px; margin: 20px 0; }
                </style>
            </head>
            <body>
                <h1 class="success">✅ 결제가 완료되었습니다!</h1>
                <div class="session-id">
                    <strong>세션 ID:</strong> %s
                </div>
                <p>결제가 성공적으로 처리되었습니다.</p>
                <p><strong>주의:</strong> 웹훅이 설정되지 않아 DB에 자동 저장되지 않습니다.</p>
            </body>
            </html>
            """.formatted(session_id);
        
        return ResponseEntity.ok()
            .header("Content-Type", "text/html; charset=UTF-8")
            .body(html);
    }
    
    @GetMapping("/payment-cancel")
    public ResponseEntity<String> paymentCancel() {
        log.info("Payment cancelled by user");
        
        String html = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>결제 취소</title>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; max-width: 600px; margin: 50px auto; padding: 20px; text-align: center; }
                    .cancel { color: #dc3545; font-size: 24px; margin-bottom: 20px; }
                </style>
            </head>
            <body>
                <h1 class="cancel">❌ 결제가 취소되었습니다</h1>
                <p>결제를 취소하셨습니다.</p>
                <button onclick="history.back()">돌아가기</button>
            </body>
            </html>
            """;
        
        return ResponseEntity.ok()
            .header("Content-Type", "text/html; charset=UTF-8")
            .body(html);
    }
}