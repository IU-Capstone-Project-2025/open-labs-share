package olsh.backend.marimoservice.model.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class UploadAssetRequestDto {
    private String assetType;
    private String componentId;
} 