package olsh.backend.api_gateway.dto.response;

import lombok.Builder;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Data
@Builder
@Schema(description = "Response object containing a list of submissions with their assets")
public class SubmissionListResponse {

    @Schema(description = "List of submissions")
    private List<SubmissionResponse> submissions;

    @Schema(description = "Total count of submissions", example = "15")
    private Long totalCount;
}
