package ac.su.kdt.bepaymentservice.kafka;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

// 외부 이벤트
// 외부 이벤트는 외부 시스템에서 발생하는 이벤트입니다.
// 이벤트는 이벤트 ID, 이벤트 유형, 사용자 ID, 팀 ID, 타임스탬프, 데이터 등의 정보를 포함합니다.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalEvent {
    
    @JsonProperty("event_id")
    private String eventId;
    
    @JsonProperty("event_type")
    private String eventType;
    
    @JsonProperty("user_id")
    private Long userId;
    
    @JsonProperty("team_id")
    private Long teamId;
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
    
    @JsonProperty("data")
    private Map<String, Object> data;
    
    public enum EventType {
        USER_REGISTERED("user.registered"),
        USER_SIGNED_UP("user.signed-up"),
        FULL_SYNC("full.sync"),
        MISSION_COMPLETED("mission.completed"),
        TEAM_CREATED("team.created"),
        ACHIEVEMENT_UNLOCKED("achievement.unlocked");
        
        private final String value;
        
        EventType(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
        
        public static EventType fromValue(String value) {
            for (EventType type : EventType.values()) {
                if (type.value.equals(value)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown event type: " + value);
        }
    }
}