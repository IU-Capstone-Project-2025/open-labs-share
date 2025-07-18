package olsh.backend.marimomanagerservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionInfoDto {
    private String id;
    private String componentId;
    private String userId;
    private String sessionName;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime lastAccessed;
    private LocalDateTime expiresAt;
    private int variableCount;
    private int executionCount;
} 