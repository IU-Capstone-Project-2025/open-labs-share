package olsh.backend.api_gateway.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetListResponse {
    @JsonProperty("total_count")
    private Long totalCount;
    
    @JsonProperty("assets")
    private List<AssetResponse> assets;
} 