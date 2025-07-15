package olsh.backend.marimoservice.model.dto;

import lombok.Data;

@Data
public class CreateComponentRequestDto {
    private String name;
    private String contentType;
    private String contentId;
    private String ownerId;
    private String initialCode;
} 