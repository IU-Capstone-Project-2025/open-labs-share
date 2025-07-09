package olsh.backend.api_gateway.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
@Schema(description = "Paginated response with tags")
public class TagListResponse {
    @Schema(description = "List of tags")
    private List<TagResponse> tags;

    @Schema(description = "Total number of tags", example = "100")
    private int count;

} 