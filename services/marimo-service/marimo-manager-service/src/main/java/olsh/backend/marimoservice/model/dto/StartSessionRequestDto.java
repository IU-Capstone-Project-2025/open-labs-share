package olsh.backend.marimoservice.model.dto;

import lombok.Data;

@Data
public class StartSessionRequestDto {
    private String componentId;
    private String userId;
    private String sessionName;
} 