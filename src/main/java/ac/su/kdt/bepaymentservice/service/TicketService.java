package ac.su.kdt.bepaymentservice.service;

import ac.su.kdt.bepaymentservice.dto.TicketDto;
import ac.su.kdt.bepaymentservice.entity.Subscription;
import ac.su.kdt.bepaymentservice.entity.SubscriptionPlan;
import ac.su.kdt.bepaymentservice.entity.TicketTransaction;
import ac.su.kdt.bepaymentservice.entity.UserTicket;
import ac.su.kdt.bepaymentservice.repository.SubscriptionRepository;
import ac.su.kdt.bepaymentservice.repository.TicketTransactionRepository;
import ac.su.kdt.bepaymentservice.repository.UserTicketRepository;
import ac.su.kdt.bepaymentservice.metrics.PaymentMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TicketService {
    
    private final UserTicketRepository userTicketRepository;
    private final TicketTransactionRepository ticketTransactionRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentEventService paymentEventService;
    private final PaymentMetrics paymentMetrics;
    
    public TicketDto getUserTickets(Long userId) {
        UserTicket userTicket = userTicketRepository.findByUserId(userId)
            .orElseGet(() -> createUserTicket(userId));
        
        return TicketDto.fromEntity(userTicket);
    }
    
    public boolean useTickets(Long userId, int ticketsToUse, Long attemptId, String reason) {
        var timer = paymentMetrics.startTicketTimer();
        UserTicket userTicket = userTicketRepository.findByUserId(userId)
            .orElseGet(() -> createUserTicket(userId));
        
        if (!userTicket.hasEnoughTickets(ticketsToUse)) {
            log.warn("User {} does not have enough tickets. Required: {}, Available: {}", 
                    userId, ticketsToUse, userTicket.getCurrentTickets());
            return false;
        }
        
        int balanceBefore = userTicket.getCurrentTickets();
        userTicket.useTickets(ticketsToUse);
        userTicket = userTicketRepository.save(userTicket);
        
        // Record transaction
        TicketTransaction transaction = TicketTransaction.builder()
            .userId(userId)
            .transactionType(TicketTransaction.TicketTransactionType.SPENT)
            .ticketAmount(-ticketsToUse)
            .balanceBefore(balanceBefore)
            .balanceAfter(userTicket.getCurrentTickets())
            .relatedAttemptId(attemptId)
            .reason(reason != null ? reason : "Mission attempt")
            .build();
        
        ticketTransactionRepository.save(transaction);
        
        // Publish ticket used event
        paymentEventService.publishTicketsUsed(userId, ticketsToUse, userTicket.getCurrentTickets());
        
        log.info("User {} used {} tickets. Balance: {} -> {}", 
                userId, ticketsToUse, balanceBefore, userTicket.getCurrentTickets());
        
        paymentMetrics.incrementTicketUsed(ticketsToUse);
        paymentMetrics.recordTicketProcessingTime(timer);
        
        return true;
    }
    
    public void refundTickets(Long userId, int ticketsToRefund, Long attemptId, String reason) {
        UserTicket userTicket = userTicketRepository.findByUserId(userId)
            .orElseGet(() -> createUserTicket(userId));
        
        int balanceBefore = userTicket.getCurrentTickets();
        userTicket.addTickets(ticketsToRefund);
        userTicket = userTicketRepository.save(userTicket);
        
        // Record transaction
        TicketTransaction transaction = TicketTransaction.builder()
            .userId(userId)
            .transactionType(TicketTransaction.TicketTransactionType.REFUND)
            .ticketAmount(ticketsToRefund)
            .balanceBefore(balanceBefore)
            .balanceAfter(userTicket.getCurrentTickets())
            .relatedAttemptId(attemptId)
            .reason(reason != null ? reason : "Ticket refund")
            .build();
        
        ticketTransactionRepository.save(transaction);
        
        // Publish ticket refunded event
        paymentEventService.publishTicketsRefunded(userId, ticketsToRefund, userTicket.getCurrentTickets());
        
        log.info("Refunded {} tickets to user {}. Balance: {} -> {}", 
                ticketsToRefund, userId, balanceBefore, userTicket.getCurrentTickets());
        
        paymentMetrics.incrementTicketRefunded(ticketsToRefund);
    }
    
    public void processTicketRefills() {
        LocalDateTime now = LocalDateTime.now();
        List<UserTicket> eligibleUsers = userTicketRepository.findUsersEligibleForRefill(now);
        
        for (UserTicket userTicket : eligibleUsers) {
            refillUserTickets(userTicket);
        }
        
        log.info("Processed ticket refills for {} users", eligibleUsers.size());
    }
    
    private void refillUserTickets(UserTicket userTicket) {
        // Get user's active subscription to determine refill amount
        List<Subscription.SubscriptionStatus> activeStatuses = List.of(
            Subscription.SubscriptionStatus.ACTIVE,
            Subscription.SubscriptionStatus.TRIAL
        );
        
        List<Subscription> activeSubscriptions = subscriptionRepository
            .findByUserIdAndStatusInOrderByCreatedAtDesc(userTicket.getUserId(), activeStatuses);
        Subscription activeSubscription = activeSubscriptions.isEmpty() ? null : activeSubscriptions.get(0);
        
        if (activeSubscription == null) {
            log.warn("No active subscription found for user: {}, skipping ticket refill", userTicket.getUserId());
            return;
        }
        
        SubscriptionPlan plan = activeSubscription.getPlan();
        int refillAmount = plan.getTicketRefillAmount();
        int ticketLimit = plan.getTicketLimit();
        
        // Don't refill if already at limit
        if (userTicket.getCurrentTickets() >= ticketLimit) {
            // Update next refill time anyway
            updateNextRefillTime(userTicket, plan.getTicketRefillIntervalHours());
            userTicketRepository.save(userTicket);
            return;
        }
        
        // Calculate how many tickets to add (don't exceed limit)
        int ticketsToAdd = Math.min(refillAmount, ticketLimit - userTicket.getCurrentTickets());
        
        if (ticketsToAdd > 0) {
            int balanceBefore = userTicket.getCurrentTickets();
            userTicket.addTickets(ticketsToAdd);
            userTicket.setLastTicketRefill(LocalDateTime.now());
            updateNextRefillTime(userTicket, plan.getTicketRefillIntervalHours());
            userTicket = userTicketRepository.save(userTicket);
            
            // Record transaction
            TicketTransaction transaction = TicketTransaction.builder()
                .userId(userTicket.getUserId())
                .transactionType(TicketTransaction.TicketTransactionType.EARNED)
                .ticketAmount(ticketsToAdd)
                .balanceBefore(balanceBefore)
                .balanceAfter(userTicket.getCurrentTickets())
                .reason("Automatic ticket refill")
                .build();
            
            ticketTransactionRepository.save(transaction);
            
            // Publish ticket refilled event
            paymentEventService.publishTicketsRefilled(userTicket.getUserId(), ticketsToAdd, userTicket.getCurrentTickets());
            
            log.info("Refilled {} tickets for user {}. Balance: {} -> {}", 
                    ticketsToAdd, userTicket.getUserId(), balanceBefore, userTicket.getCurrentTickets());
        } else {
            updateNextRefillTime(userTicket, plan.getTicketRefillIntervalHours());
            userTicketRepository.save(userTicket);
        }
    }
    
    private void updateNextRefillTime(UserTicket userTicket, int intervalHours) {
        userTicket.setNextRefillAt(LocalDateTime.now().plusHours(intervalHours));
    }
    
    private UserTicket createUserTicket(Long userId) {
        // Get user's subscription plan to set initial tickets
        List<Subscription.SubscriptionStatus> activeStatuses = List.of(
            Subscription.SubscriptionStatus.ACTIVE,
            Subscription.SubscriptionStatus.TRIAL
        );
        
        List<Subscription> activeSubscriptions = subscriptionRepository
            .findByUserIdAndStatusInOrderByCreatedAtDesc(userId, activeStatuses);
        Subscription activeSubscription = activeSubscriptions.isEmpty() ? null : activeSubscriptions.get(0);
        
        int initialTickets = 0;
        LocalDateTime nextRefillAt = null;
        
        if (activeSubscription != null) {
            SubscriptionPlan plan = activeSubscription.getPlan();
            initialTickets = plan.getTicketRefillAmount();
            nextRefillAt = LocalDateTime.now().plusHours(plan.getTicketRefillIntervalHours());
        }
        
        UserTicket userTicket = UserTicket.builder()
            .userId(userId)
            .currentTickets(initialTickets)
            .lastTicketRefill(LocalDateTime.now())
            .nextRefillAt(nextRefillAt)
            .build();
        
        userTicket = userTicketRepository.save(userTicket);
        
        if (initialTickets > 0) {
            // Record initial ticket grant
            TicketTransaction transaction = TicketTransaction.builder()
                .userId(userId)
                .transactionType(TicketTransaction.TicketTransactionType.EARNED)
                .ticketAmount(initialTickets)
                .balanceBefore(0)
                .balanceAfter(initialTickets)
                .reason("Initial ticket grant")
                .build();
            
            ticketTransactionRepository.save(transaction);
        }
        
        log.info("Created ticket account for user {} with {} initial tickets", userId, initialTickets);
        return userTicket;
    }
    
    public void adjustTickets(Long userId, int adjustment, String reason) {
        UserTicket userTicket = userTicketRepository.findByUserId(userId)
            .orElseGet(() -> createUserTicket(userId));
        
        int balanceBefore = userTicket.getCurrentTickets();
        
        if (adjustment > 0) {
            userTicket.addTickets(adjustment);
        } else {
            userTicket.useTickets(Math.abs(adjustment));
        }
        
        userTicket = userTicketRepository.save(userTicket);
        
        // Record transaction
        TicketTransaction transaction = TicketTransaction.builder()
            .userId(userId)
            .transactionType(TicketTransaction.TicketTransactionType.ADMIN_ADJUST)
            .ticketAmount(adjustment)
            .balanceBefore(balanceBefore)
            .balanceAfter(userTicket.getCurrentTickets())
            .reason(reason != null ? reason : "Admin adjustment")
            .build();
        
        ticketTransactionRepository.save(transaction);
        
        log.info("Admin adjusted tickets for user {} by {}. Balance: {} -> {}", 
                userId, adjustment, balanceBefore, userTicket.getCurrentTickets());
        
        if (adjustment > 0) {
            paymentMetrics.incrementTicketGranted(adjustment);
        } else {
            paymentMetrics.incrementTicketUsed(Math.abs(adjustment));
        }
    }
}