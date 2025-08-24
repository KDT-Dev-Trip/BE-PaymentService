package ac.su.kdt.bepaymentservice.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/payment")
public class PaymentPageController {
    
    @Value("${toss.payments.client.key}")
    private String clientKey;
    
    @GetMapping("/checkout")
    public String checkoutPage(
            @RequestParam(defaultValue = "29900") Long amount,
            @RequestParam(required = false) String orderId,
            @RequestParam(defaultValue = "구독 결제") String orderName,
            @RequestParam(defaultValue = "test@example.com") String customerEmail,
            @RequestParam(defaultValue = "테스트 사용자") String customerName,
            Model model) {
        
        // Generate orderId if not provided
        if (orderId == null || orderId.trim().isEmpty()) {
            orderId = "order_" + System.currentTimeMillis();
        }
        
        model.addAttribute("amount", amount);
        model.addAttribute("orderId", orderId);
        model.addAttribute("orderName", orderName);
        model.addAttribute("customerEmail", customerEmail);
        model.addAttribute("customerName", customerName);
        model.addAttribute("clientKey", clientKey);
        
        return "checkout";
    }
    
    @GetMapping("/success")
    public String successPage(
            @RequestParam String paymentKey,
            @RequestParam String orderId,
            @RequestParam Long amount,
            Model model) {
        
        model.addAttribute("paymentKey", paymentKey);
        model.addAttribute("orderId", orderId);
        model.addAttribute("amount", amount);
        
        return "success";
    }
    
    @GetMapping("/fail")
    public String failPage(
            @RequestParam String code,
            @RequestParam String message,
            @RequestParam String orderId,
            Model model) {
        
        model.addAttribute("errorCode", code);
        model.addAttribute("errorMessage", message);
        model.addAttribute("orderId", orderId);
        
        return "fail";
    }
}