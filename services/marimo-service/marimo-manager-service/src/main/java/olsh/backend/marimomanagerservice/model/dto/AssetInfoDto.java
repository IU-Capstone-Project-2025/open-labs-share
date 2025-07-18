package olsh.backend.marimomanagerservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssetInfoDto {
    private String id;
    private String componentId;
    private String assetType;
    private String fileName;
    private String filePath;
    private String mimeType;
    private long fileSize;
    private LocalDateTime createdAt;
    private Map<String, Object> metadata;
    private String downloadUrl;
    private String thumbnailUrl;
} 