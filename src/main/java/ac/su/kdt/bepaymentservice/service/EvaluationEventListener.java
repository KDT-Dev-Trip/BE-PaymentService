package ac.su.kdt.bepaymentservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EvaluationEventListener {
    
    private final PaymentService paymentService;
    private final PaymentEventPublisher paymentEventPublisher;
    
    /**
     * AI 평가 이벤트 처리 - 결제 서비스 관점
     */
    @KafkaListener(topics = "evaluation-events", groupId = "payment-service-evaluation-group")
    public void handleEvaluationEvent(Map<String, Object> eventData) {
        String eventType = (String) eventData.get("eventType");
        
        try {
            switch (eventType) {
                case "evaluation.started":
                    handleEvaluationStarted(eventData);
                    break;
                case "evaluation.failed":
                    handleEvaluationFailed(eventData);
                    break;
                case "evaluation.retry-requested":
                    handleEvaluationRetryRequested(eventData);
                    break;
                case "evaluation.retry-completed":
                    handleEvaluationRetryCompleted(eventData);
                    break;
                default:
                    log.debug("Unknown evaluation event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Error handling evaluation event: {}", eventType, e);
        }
    }
    
    /**
     * 평가 시작 이벤트 처리
     * - 평가 비용 기록 및 추적
     */
    private void handleEvaluationStarted(Map<String, Object> eventData) {
        String evaluationId = (String) eventData.get("evaluationId");
        String missionId = (String) eventData.get("missionId");
        String missionTitle = (String) eventData.get("missionTitle");
        Long userId = Long.valueOf(eventData.get("userId").toString());
        String evaluationType = (String) eventData.get("evaluationType");
        Integer commandLogCount = (Integer) eventData.get("commandLogCount");
        
        log.info("🚀 [PAYMENT_SERVICE] Processing evaluation started event: evaluationId={}, userId={}, evaluationType={}", 
            evaluationId, userId, evaluationType);
        
        try {
            // 평가 비용 계산 및 기록 (실제 차감은 하지 않음)
            BigDecimal evaluationCost = calculateEvaluationCost(evaluationType, commandLogCount);
            
            // 평가 비용 사용 이력 기록
            paymentService.recordEvaluationCostUsage(userId, evaluationId, missionId, evaluationCost, "STARTED");
            
            log.info("📊 [PAYMENT_SERVICE] Evaluation cost recorded: userId={}, evaluationId={}, cost={}", 
                userId, evaluationId, evaluationCost);
            
        } catch (Exception e) {
            log.error("❌ [PAYMENT_SERVICE] Failed to process evaluation started event: evaluationId={}, userId={}", 
                evaluationId, userId, e);
        }
    }
    
    /**
     * 평가 실패 이벤트 처리
     * - 실패 이력 기록
     */
    private void handleEvaluationFailed(Map<String, Object> eventData) {
        String evaluationId = (String) eventData.get("evaluationId");
        String missionId = (String) eventData.get("missionId");
        Long userId = Long.valueOf(eventData.get("userId").toString());
        String failureReason = (String) eventData.get("failureReason");
        String errorCode = (String) eventData.get("errorCode");
        Integer retryAttempt = (Integer) eventData.get("retryAttempt");
        
        log.warn("🚨 [PAYMENT_SERVICE] Processing evaluation failed event: evaluationId={}, userId={}, reason={}", 
            evaluationId, userId, failureReason);
        
        try {
            // 평가 실패 이력 기록
            paymentService.recordEvaluationFailureHistory(userId, evaluationId, missionId, failureReason, retryAttempt);
            
            log.info("📝 [PAYMENT_SERVICE] Evaluation failure recorded: userId={}, evaluationId={}", 
                userId, evaluationId);
            
        } catch (Exception e) {
            log.error("❌ [PAYMENT_SERVICE] Failed to process evaluation failed event: evaluationId={}, userId={}", 
                evaluationId, userId, e);
        }
    }
    
    /**
     * 평가 재시도 요청 이벤트 처리
     * - 재시도 비용 기록
     */
    private void handleEvaluationRetryRequested(Map<String, Object> eventData) {
        String evaluationId = (String) eventData.get("evaluationId");
        String originalEvaluationId = (String) eventData.get("originalEvaluationId");
        String missionId = (String) eventData.get("missionId");
        Long userId = Long.valueOf(eventData.get("userId").toString());
        Integer retryAttempt = (Integer) eventData.get("retryAttempt");
        String retryStrategy = (String) eventData.get("retryStrategy");
        
        log.info("🔄 [PAYMENT_SERVICE] Processing evaluation retry requested event: evaluationId={}, userId={}, retryAttempt={}", 
            evaluationId, userId, retryAttempt);
        
        try {
            // 재시도 비용 계산 및 기록
            BigDecimal retryCost = calculateRetryCost(retryAttempt, retryStrategy);
            
            paymentService.recordEvaluationRetryCost(userId, evaluationId, missionId, retryCost, retryAttempt);
            
            log.info("📊 [PAYMENT_SERVICE] Retry cost recorded: userId={}, evaluationId={}, cost={}, retryAttempt={}", 
                userId, evaluationId, retryCost, retryAttempt);
            
        } catch (Exception e) {
            log.error("❌ [PAYMENT_SERVICE] Failed to process evaluation retry requested event: evaluationId={}, userId={}", 
                evaluationId, userId, e);
        }
    }
    
    /**
     * 평가 재시도 완료 이벤트 처리
     * - 최종 평가 비용 정산 기록
     */
    private void handleEvaluationRetryCompleted(Map<String, Object> eventData) {
        String evaluationId = (String) eventData.get("evaluationId");
        String originalEvaluationId = (String) eventData.get("originalEvaluationId");
        String missionId = (String) eventData.get("missionId");
        Long userId = Long.valueOf(eventData.get("userId").toString());
        Integer retryAttempt = (Integer) eventData.get("retryAttempt");
        String retryStatus = (String) eventData.get("retryStatus");
        Integer finalScore = (Integer) eventData.get("finalScore");
        
        log.info("✅ [PAYMENT_SERVICE] Processing evaluation retry completed event: evaluationId={}, userId={}, status={}", 
            evaluationId, userId, retryStatus);
        
        try {
            // 최종 평가 완료 기록
            paymentService.recordFinalEvaluationCompletion(userId, evaluationId, originalEvaluationId, 
                retryStatus, finalScore, retryAttempt);
            
            // 성과 기반 보너스 크레딧 지급 (높은 점수 시)
            if ("SUCCESS".equals(retryStatus) && finalScore != null && finalScore >= 90) {
                BigDecimal bonus = BigDecimal.valueOf(5.0); // 고정 보너스 크레딧
                paymentService.addBonusCredit(userId, bonus, "HIGH_SCORE_BONUS", 
                    String.format("평가 우수 성과 보너스 (점수: %d점)", finalScore));
                
                log.info("🎉 [PAYMENT_SERVICE] Bonus credit awarded for high score: userId={}, bonus={}, score={}", 
                    userId, bonus, finalScore);
            }
            
            log.info("✅ [PAYMENT_SERVICE] Evaluation completion recorded: userId={}, evaluationId={}, status={}", 
                userId, evaluationId, retryStatus);
            
        } catch (Exception e) {
            log.error("❌ [PAYMENT_SERVICE] Failed to process evaluation retry completed event: evaluationId={}, userId={}", 
                evaluationId, userId, e);
        }
    }
    
    /**
     * 평가 비용 계산
     */
    private BigDecimal calculateEvaluationCost(String evaluationType, Integer commandLogCount) {
        BigDecimal baseCost = BigDecimal.valueOf(1.0); // 기본 평가 비용 (크레딧 단위)
        
        if (commandLogCount != null && commandLogCount > 50) {
            // 명령어 수가 많으면 추가 비용
            baseCost = baseCost.add(BigDecimal.valueOf((commandLogCount - 50) * 0.01));
        }
        
        if ("AI_DETAILED".equals(evaluationType)) {
            return baseCost.multiply(BigDecimal.valueOf(2.0)); // 상세 평가는 2배
        }
        
        return baseCost;
    }
    
    /**
     * 재시도 비용 계산 (첫 번째 재시도는 무료)
     */
    private BigDecimal calculateRetryCost(Integer retryAttempt, String retryStrategy) {
        if (retryAttempt == null || retryAttempt == 1) {
            return BigDecimal.ZERO; // 첫 번째 재시도는 무료
        }
        
        return BigDecimal.valueOf(0.5); // 추가 재시도는 0.5 크레딧
    }
}