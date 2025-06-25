package olsh.backend.api_gateway.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetResponse {
    @JsonProperty("asset_id")
    private Long assetId;
    
    @JsonProperty("lab_id") 
    private Long labId;
    
    @JsonProperty("filename")
    private String filename;
    
    @JsonProperty("total_size")
    private Long totalSize;
    
    @JsonProperty("upload_date")
    private String uploadDate;
} 