package ac.su.kdt.bepaymentservice.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Gateway 인증 유틸리티
 * Gateway에서 전달된 사용자 정보에 접근하는 유틸리티 클래스
 */
@Slf4j
public class GatewayAuthUtils {
    
    private static final String USER_ID_ATTRIBUTE = "gateway.user.id";
    private static final String USER_EMAIL_ATTRIBUTE = "gateway.user.email";
    
    /**
     * 현재 요청에서 사용자 ID를 가져옵니다
     * @return 사용자 ID 또는 null
     */
    public static String getCurrentUserId() {
        HttpServletRequest request = getCurrentRequest();
        if (request != null) {
            String userId = (String) request.getAttribute(USER_ID_ATTRIBUTE);
            log.debug("Retrieved user ID from gateway: {}", userId);
            return userId;
        }
        return null;
    }
    
    /**
     * 현재 요청에서 사용자 이메일을 가져옵니다
     * @return 사용자 이메일 또는 null
     */
    public static String getCurrentUserEmail() {
        HttpServletRequest request = getCurrentRequest();
        if (request != null) {
            String userEmail = (String) request.getAttribute(USER_EMAIL_ATTRIBUTE);
            log.debug("Retrieved user email from gateway: {}", userEmail);
            return userEmail;
        }
        return null;
    }
    
    /**
     * 현재 요청에서 사용자 ID를 Long 타입으로 가져옵니다
     * @return 사용자 ID (Long) 또는 null
     */
    public static Long getCurrentUserIdAsLong() {
        String userId = getCurrentUserId();
        if (userId != null) {
            try {
                return Long.parseLong(userId);
            } catch (NumberFormatException e) {
                log.warn("Failed to parse user ID as Long: {}", userId);
                return null;
            }
        }
        return null;
    }
    
    /**
     * 사용자가 인증되었는지 확인합니다
     * @return 인증 여부
     */
    public static boolean isAuthenticated() {
        return getCurrentUserId() != null && getCurrentUserEmail() != null;
    }
    
    /**
     * 특정 사용자 ID가 현재 인증된 사용자와 일치하는지 확인합니다
     * @param userId 확인할 사용자 ID (Long)
     * @return 일치 여부
     */
    public static boolean isCurrentUser(Long userId) {
        Long currentUserId = getCurrentUserIdAsLong();
        return currentUserId != null && currentUserId.equals(userId);
    }
    
    /**
     * 특정 사용자 ID를 Long으로 변환하여 현재 인증된 사용자와 비교합니다
     * @param userId 확인할 사용자 ID (String, UUID 형태)
     * @return 일치 여부
     */
    public static boolean isCurrentUserByHash(String userId) {
        String currentUserId = getCurrentUserId();
        if (currentUserId != null && userId != null) {
            // 해시 값으로 비교 (같은 알고리즘 사용)
            long currentHash = Math.abs((long) currentUserId.hashCode());
            long targetHash = Math.abs((long) userId.hashCode());
            return currentHash == targetHash;
        }
        return false;
    }
    
    /**
     * 특정 사용자 ID가 현재 인증된 사용자와 일치하는지 확인합니다
     * @param userId 확인할 사용자 ID (String)
     * @return 일치 여부
     */
    public static boolean isCurrentUser(String userId) {
        String currentUserId = getCurrentUserId();
        return currentUserId != null && currentUserId.equals(userId);
    }
    
    /**
     * 현재 HttpServletRequest를 가져옵니다
     * @return HttpServletRequest 또는 null
     */
    private static HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attributes = 
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                return attributes.getRequest();
            }
        } catch (Exception e) {
            log.debug("Failed to get current request: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * 현재 인증된 사용자의 정보를 문자열로 반환합니다
     * @return 사용자 정보 문자열
     */
    public static String getCurrentUserInfo() {
        if (isAuthenticated()) {
            return String.format("User(id=%s, email=%s)", getCurrentUserId(), getCurrentUserEmail());
        }
        return "Anonymous User";
    }
}