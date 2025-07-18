package olsh.backend.marimomanagerservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "component_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComponentSession {

    @Id
    @Column(name = "id")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "component_id", nullable = false)
    private Component component;

    @Column(name = "session_name")
    private String sessionName;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status")
    private SessionStatus status = SessionStatus.ACTIVE;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "state_data", columnDefinition = "JSONB")
    private String stateData;

    @Column(name = "python_process_id")
    private String pythonProcessId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "last_accessed")
    private LocalDateTime lastAccessed;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    public enum SessionStatus {
        ACTIVE, IDLE, EXPIRED
    }
} 