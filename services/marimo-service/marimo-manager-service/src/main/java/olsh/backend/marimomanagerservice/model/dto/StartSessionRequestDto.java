package olsh.backend.marimomanagerservice.model.dto;

import lombok.Data;

@Data
public class StartSessionRequestDto {
    private String componentId;
    private String sessionName;
} 