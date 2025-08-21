package ac.su.kdt.bepaymentservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * API Gateway 인증 설정
 * Gateway에서 전달된 사용자 정보를 처리합니다.
 */
@Slf4j
@Configuration
public class GatewayAuthConfig {
    
    /**
     * Gateway 인증 필터
     * X-User-Id, X-User-Email 헤더에서 사용자 정보를 추출하여 처리합니다.
     */
    @Bean
    public OncePerRequestFilter gatewayAuthFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(
                    HttpServletRequest request, 
                    HttpServletResponse response, 
                    FilterChain filterChain) throws ServletException, IOException {
                
                // Gateway에서 전달된 사용자 정보 추출
                String userId = request.getHeader("X-User-Id");
                String userEmail = request.getHeader("X-User-Email");
                
                if (userId != null && userEmail != null) {
                    // 사용자 정보를 request attribute에 저장
                    request.setAttribute("gateway.user.id", userId);
                    request.setAttribute("gateway.user.email", userEmail);
                    
                    log.debug("Gateway authentication - User ID: {}, Email: {}", userId, userEmail);
                } else {
                    log.debug("No gateway authentication headers found");
                }
                
                filterChain.doFilter(request, response);
            }
            
            @Override
            protected boolean shouldNotFilter(HttpServletRequest request) {
                String path = request.getRequestURI();
                // actuator, health check 등은 인증 제외
                return path.startsWith("/actuator") || 
                       path.startsWith("/health") ||
                       path.startsWith("/metrics");
            }
        };
    }
}