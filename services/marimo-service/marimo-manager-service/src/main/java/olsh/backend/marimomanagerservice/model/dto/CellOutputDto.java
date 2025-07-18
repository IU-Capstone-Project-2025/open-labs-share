package olsh.backend.marimomanagerservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CellOutputDto {
    private String outputId;
    private String type;
    private String content;
    private byte[] data;
    private String mimeType;
    private Map<String, String> metadata;
    private String assetUrl;
    private String dataType;
} 