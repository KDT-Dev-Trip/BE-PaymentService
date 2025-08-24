package ac.su.kdt.bepaymentservice.controller;

import ac.su.kdt.bepaymentservice.service.TossPaymentsService;
import ac.su.kdt.bepaymentservice.toss.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/toss")
@RequiredArgsConstructor
@Slf4j
public class TossPaymentsController {
    
    private final TossPaymentsService tossPaymentsService;
    
    @PostMapping("/billing/issue")
    public ResponseEntity<BillingResponse> issueBillingKey(@RequestBody BillingKeyRequest request) {
        log.info("Billing key issue request received for customerKey: {}", request.getCustomerKey());
        
        try {
            BillingResponse response = tossPaymentsService.issueBillingKey(request).block();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error issuing billing key: ", e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping("/billing/{billingKey}/payment")
    public ResponseEntity<PaymentResponse> processAutoPayment(
            @PathVariable String billingKey,
            @RequestBody AutoPaymentRequest request) {
        
        log.info("Auto payment request received for billingKey: {}, orderId: {}", 
                billingKey, request.getOrderId());
        
        try {
            PaymentResponse response = tossPaymentsService.processAutoPayment(billingKey, request).block();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing auto payment: ", e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping("/payments/checkout")
    public ResponseEntity<CheckoutResponse> createCheckout(@RequestBody CheckoutRequest request) {
        log.info("Checkout request received for amount: {}, orderId: {}", 
                request.getAmount(), request.getOrderId());
        
        try {
            CheckoutResponse response = tossPaymentsService.createCheckout(request).block();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error creating checkout: ", e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping("/payments/confirm")
    public ResponseEntity<PaymentResponse> confirmPayment(@RequestBody ConfirmRequest request) {
        log.info("Payment confirmation request received for paymentKey: {}", request.getPaymentKey());
        
        try {
            PaymentResponse response = tossPaymentsService.confirmPayment(request).block();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error confirming payment: ", e);
            return ResponseEntity.badRequest().build();
        }
    }
}