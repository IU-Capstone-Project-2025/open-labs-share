package olsh.backend.marimomanagerservice.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Widget version DTO
 */
public class WidgetVersionDto {
    
    @JsonProperty("widgetId")
    private String widgetId;
    
    @JsonProperty("version")
    private int version;
    
    @JsonProperty("value")
    private Object value;
    
    @JsonProperty("userId")
    private String userId;
    
    @JsonProperty("timestamp")
    private long timestamp;
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
    
    @JsonProperty("changeReason")
    private String changeReason;

    // Getters and setters
    public String getWidgetId() {
        return widgetId;
    }

    public void setWidgetId(String widgetId) {
        this.widgetId = widgetId;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public String getChangeReason() {
        return changeReason;
    }

    public void setChangeReason(String changeReason) {
        this.changeReason = changeReason;
    }
}
