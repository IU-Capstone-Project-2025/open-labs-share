package olsh.backend.marimomanagerservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "execution_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ComponentSession session;

    @Column(name = "cell_id")
    private String cellId;

    @Lob
    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "success", nullable = false)
    private boolean success;

    @Lob
    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "execution_time_ms")
    private long executionTimeMs;
    
    @Column(name = "output_count")
    private int outputCount;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
} 