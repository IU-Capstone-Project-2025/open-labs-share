package olsh.backend.marimoservice.model.dto;

import lombok.Data;

@Data
public class ExecuteCellRequestDto {
    private String cellId;
    private String code;
} 