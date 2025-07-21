package olsh.backend.marimomanagerservice.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "widget_states")
public class WidgetState {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false)
    private String sessionId;
    
    @Column(nullable = false)
    private String widgetId;
    
    @Column(nullable = false)
    private String widgetType;
    
    @Column(columnDefinition = "TEXT")
    private String value;
    
    @Column(columnDefinition = "JSONB")
    private String properties;
    
    @Column(columnDefinition = "JSONB")
    private String constraints;
    
    @Column
    private String lastUpdatedBy;
    
    @Column
    private Integer version;
    
    @Column
    private Boolean active;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Indexes for efficient queries
    @Table(indexes = {
        @Index(name = "idx_widget_state_session", columnList = "sessionId"),
        @Index(name = "idx_widget_state_widget", columnList = "widgetId"),
        @Index(name = "idx_widget_state_session_widget", columnList = "sessionId, widgetId"),
        @Index(name = "idx_widget_state_active", columnList = "active")
    })
    public static class IndexDefinition {}

    // Constructors
    public WidgetState() {}

    public WidgetState(String sessionId, String widgetId, String widgetType, String value) {
        this.sessionId = sessionId;
        this.widgetId = widgetId;
        this.widgetType = widgetType;
        this.value = value;
        this.active = true;
        this.version = 1;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getWidgetId() { return widgetId; }
    public void setWidgetId(String widgetId) { this.widgetId = widgetId; }

    public String getWidgetType() { return widgetType; }
    public void setWidgetType(String widgetType) { this.widgetType = widgetType; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public String getProperties() { return properties; }
    public void setProperties(String properties) { this.properties = properties; }

    public String getConstraints() { return constraints; }
    public void setConstraints(String constraints) { this.constraints = constraints; }

    public String getLastUpdatedBy() { return lastUpdatedBy; }
    public void setLastUpdatedBy(String lastUpdatedBy) { this.lastUpdatedBy = lastUpdatedBy; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
