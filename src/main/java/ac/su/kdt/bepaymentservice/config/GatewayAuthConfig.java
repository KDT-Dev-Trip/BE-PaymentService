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
                String userRole = request.getHeader("X-User-Role");
                String gatewayRoute = request.getHeader("X-Gateway-Route");
                
                if (gatewayRoute != null) {
                    log.info("Gateway request detected - Route: {}, User: {}, Email: {}, Role: {}", 
                            gatewayRoute, userId, userEmail, userRole);
                    
                    // 헤더 정보를 request attributes에 저장하여 컨트롤러에서 사용할 수 있게 함
                    if (userId != null) {
                        request.setAttribute("gateway.user.id", userId);
                    }
                    if (userEmail != null) {
                        request.setAttribute("gateway.user.email", userEmail);
                    }
                    if (userRole != null) {
                        request.setAttribute("gateway.user.role", userRole);
                    }
                    request.setAttribute("gateway.authenticated", true);
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