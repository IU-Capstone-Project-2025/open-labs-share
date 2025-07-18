package olsh.backend.marimomanagerservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VariableInfoDto {
    private String name;
    private String type;
    private String value;
    private long sizeBytes;
    private Map<String, String> metadata;
} 