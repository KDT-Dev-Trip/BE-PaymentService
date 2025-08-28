package ac.su.kdt.bepaymentservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    /**
     * Payment 이벤트 토픽 - 결제 관련 이벤트
     */
    @Bean
    public NewTopic paymentEventsTopic() {
        return TopicBuilder
                .name("payment-events")
                .partitions(3)
                .replicas(1) // 단일 브로커 환경이므로 1로 설정
                .build();
    }

    /**
     * Subscription 이벤트 토픽 - 구독 관련 이벤트 (기존 호환성)
     */
    @Bean
    public NewTopic subscriptionEventsTopic() {
        return TopicBuilder
                .name("subscription-events")
                .partitions(3)
                .replicas(1) // 단일 브로커 환경이므로 1로 설정
                .build();
    }

    /**
     * User 이벤트 토픽 - 사용자 관련 이벤트
     */
    @Bean
    public NewTopic userEventsTopic() {
        return TopicBuilder
                .name("user-events")
                .partitions(3)
                .replicas(1) // 단일 브로커 환경이므로 1로 설정
                .build();
    }

    /**
     * Auth 이벤트 토픽 - 인증/권한 관련 이벤트
     */
    @Bean
    public NewTopic authEventsTopic() {
        return TopicBuilder
                .name("auth-events")
                .partitions(3)
                .replicas(1) // 단일 브로커 환경이므로 1로 설정
                .build();
    }

    /**
     * Mission 이벤트 토픽 - 미션 관련 이벤트
     */
    @Bean
    public NewTopic missionEventsTopic() {
        return TopicBuilder
                .name("mission-events")
                .partitions(3)
                .replicas(1) // 단일 브로커 환경이므로 1로 설정
                .build();
    }
}