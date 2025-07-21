package olsh.backend.api_gateway.dto.response;

import lombok.Builder;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Data
@Builder
@Schema(description = "Response object containing a paginated list of laboratory works")
public class LabListResponse {
    @Schema(description = "List of labs on the current page")
    private List<LabResponse> labs;

    @Schema(description = "List of tags associated with the labs")
    private List<TagResponse> tags;

    @Schema(description = "Total number of items across all pages", example = "100")
    private Integer count;

}
