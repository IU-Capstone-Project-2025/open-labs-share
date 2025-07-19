package olsh.backend.marimomanagerservice.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Widget collaboration DTO
 */
public class WidgetCollaborationDto {
    
    @JsonProperty("sessionId")
    private String sessionId;
    
    @JsonProperty("collaborators")
    private List<CollaboratorDto> collaborators;
    
    @JsonProperty("widgetLocks")
    private Map<String, WidgetLockDto> widgetLocks;
    
    @JsonProperty("recentActivity")
    private List<CollaborationActivityDto> recentActivity;

    // Getters and setters
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public List<CollaboratorDto> getCollaborators() {
        return collaborators;
    }

    public void setCollaborators(List<CollaboratorDto> collaborators) {
        this.collaborators = collaborators;
    }

    public Map<String, WidgetLockDto> getWidgetLocks() {
        return widgetLocks;
    }

    public void setWidgetLocks(Map<String, WidgetLockDto> widgetLocks) {
        this.widgetLocks = widgetLocks;
    }

    public List<CollaborationActivityDto> getRecentActivity() {
        return recentActivity;
    }

    public void setRecentActivity(List<CollaborationActivityDto> recentActivity) {
        this.recentActivity = recentActivity;
    }

    // Nested DTOs
    public static class CollaboratorDto {
        @JsonProperty("userId")
        private String userId;
        
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("avatar")
        private String avatar;
        
        @JsonProperty("joinedAt")
        private long joinedAt;
        
        @JsonProperty("lastSeen")
        private long lastSeen;

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getAvatar() {
            return avatar;
        }

        public void setAvatar(String avatar) {
            this.avatar = avatar;
        }

        public long getJoinedAt() {
            return joinedAt;
        }

        public void setJoinedAt(long joinedAt) {
            this.joinedAt = joinedAt;
        }

        public long getLastSeen() {
            return lastSeen;
        }

        public void setLastSeen(long lastSeen) {
            this.lastSeen = lastSeen;
        }
    }

    public static class WidgetLockDto {
        @JsonProperty("widgetId")
        private String widgetId;
        
        @JsonProperty("userId")
        private String userId;
        
        @JsonProperty("timestamp")
        private long timestamp;

        public String getWidgetId() {
            return widgetId;
        }

        public void setWidgetId(String widgetId) {
            this.widgetId = widgetId;
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
    }

    public static class CollaborationActivityDto {
        @JsonProperty("userId")
        private String userId;
        
        @JsonProperty("action")
        private String action;
        
        @JsonProperty("widgetId")
        private String widgetId;
        
        @JsonProperty("timestamp")
        private long timestamp;
        
        @JsonProperty("details")
        private Map<String, Object> details;

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public String getWidgetId() {
            return widgetId;
        }

        public void setWidgetId(String widgetId) {
            this.widgetId = widgetId;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

        public Map<String, Object> getDetails() {
            return details;
        }

        public void setDetails(Map<String, Object> details) {
            this.details = details;
        }
    }
}
