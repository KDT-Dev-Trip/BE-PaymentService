package ac.su.kdt.bepaymentservice.kafka;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

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