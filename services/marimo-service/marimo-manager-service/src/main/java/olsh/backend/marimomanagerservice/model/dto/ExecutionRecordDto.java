package olsh.backend.marimomanagerservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionRecordDto {
    private String id;
    private String sessionId;
    private String cellId;
    private String code;
    private boolean success;
    private String errorMessage;
    private long executionTimeMs;
    private LocalDateTime timestamp;
    private int outputCount;
} 