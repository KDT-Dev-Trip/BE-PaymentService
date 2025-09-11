package ac.su.kdt.bepaymentservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Component
public class UserServiceClient {
    
    private final RestTemplate restTemplate;
    private final String userServiceUrl;
    
    public UserServiceClient(RestTemplate restTemplate,
                            @Value("${app.external-services.user-service.url:http://localhost:8082}") String userServiceUrl) {
        this.restTemplate = restTemplate;
        this.userServiceUrl = userServiceUrl;
        log.info("UserServiceClient initialized - URL: {}", userServiceUrl);
    }
    
    /**
     * 사용자 티켓 정보 조회
     */
    public Map<String, Object> getUserTickets(Long userId) {
        try {
            String url = userServiceUrl + "/api/tickets/users/" + userId;
            log.info("Getting user tickets for user: {} from URL: {}", userId, url);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Void> request = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.GET, request, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Successfully retrieved tickets for user: {}", userId);
                return response.getBody();
            } else {
                log.warn("Failed to get tickets for user: {}, status: {}", userId, response.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            log.error("Error getting tickets for user: {}", userId, e);
            return null;
        }
    }
    
    /**
     * 티켓 사용
     */
    public boolean useTickets(Long userId, int amount, Long attemptId, String reason) {
        try {
            String url = userServiceUrl + "/api/tickets/users/" + userId + "/use";
            log.info("Using {} tickets for user: {}", amount, userId);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // URL 파라미터로 전송
            String fullUrl = url + "?amount=" + amount;
            if (attemptId != null) {
                fullUrl += "&attemptId=" + attemptId;
            }
            if (reason != null && !reason.isEmpty()) {
                fullUrl += "&reason=" + reason;
            }
            
            HttpEntity<Void> request = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(fullUrl, request, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Boolean success = (Boolean) response.getBody().get("success");
                log.info("Ticket usage result for user {}: {}", userId, success);
                return success != null ? success : false;
            } else {
                log.warn("Failed to use tickets for user: {}, status: {}", userId, response.getStatusCode());
                return false;
            }
        } catch (Exception e) {
            log.error("Error using tickets for user: {}", userId, e);
            return false;
        }
    }
    
    /**
     * 티켓 환불
     */
    public boolean refundTickets(Long userId, int amount, Long attemptId, String reason) {
        try {
            String url = userServiceUrl + "/api/tickets/users/" + userId + "/refund";
            log.info("Refunding {} tickets for user: {}", amount, userId);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // URL 파라미터로 전송
            String fullUrl = url + "?amount=" + amount;
            if (attemptId != null) {
                fullUrl += "&attemptId=" + attemptId;
            }
            if (reason != null && !reason.isEmpty()) {
                fullUrl += "&reason=" + reason;
            }
            
            HttpEntity<Void> request = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(fullUrl, request, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Boolean success = (Boolean) response.getBody().get("success");
                log.info("Ticket refund result for user {}: {}", userId, success);
                return success != null ? success : false;
            } else {
                log.warn("Failed to refund tickets for user: {}, status: {}", userId, response.getStatusCode());
                return false;
            }
        } catch (Exception e) {
            log.error("Error refunding tickets for user: {}", userId, e);
            return false;
        }
    }
    
    /**
     * 티켓 조정 (관리자)
     */
    public boolean adjustTickets(Long userId, int adjustment, String reason) {
        try {
            String url = userServiceUrl + "/api/tickets/users/" + userId + "/adjust";
            log.info("Adjusting {} tickets for user: {}", adjustment, userId);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // URL 파라미터로 전송
            String fullUrl = url + "?adjustment=" + adjustment;
            if (reason != null && !reason.isEmpty()) {
                fullUrl += "&reason=" + reason;
            }
            
            HttpEntity<Void> request = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(fullUrl, request, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Boolean success = (Boolean) response.getBody().get("success");
                log.info("Ticket adjustment result for user {}: {}", userId, success);
                return success != null ? success : false;
            } else {
                log.warn("Failed to adjust tickets for user: {}, status: {}", userId, response.getStatusCode());
                return false;
            }
        } catch (Exception e) {
            log.error("Error adjusting tickets for user: {}", userId, e);
            return false;
        }
    }
}