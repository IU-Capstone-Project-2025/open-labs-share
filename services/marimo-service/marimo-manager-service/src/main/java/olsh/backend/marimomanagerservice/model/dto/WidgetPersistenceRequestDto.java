package olsh.backend.marimomanagerservice.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Widget persistence request DTO
 */
public class WidgetPersistenceRequestDto {
    
    @JsonProperty("sessionId")
    private String sessionId;
    
    @JsonProperty("widgetId")
    private String widgetId;
    
    @JsonProperty("state")
    private Map<String, Object> state;
    
    @JsonProperty("strategy")
    private String strategy = "local";
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    // Getters and setters
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getWidgetId() {
        return widgetId;
    }

    public void setWidgetId(String widgetId) {
        this.widgetId = widgetId;
    }

    public Map<String, Object> getState() {
        return state;
    }

    public void setState(Map<String, Object> state) {
        this.state = state;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
