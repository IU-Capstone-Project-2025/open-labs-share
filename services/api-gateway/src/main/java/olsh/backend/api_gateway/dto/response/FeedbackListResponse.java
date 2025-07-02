package olsh.backend.api_gateway.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response object containing a paginated list of feedbacks")
public class FeedbackListResponse {
    @Schema(description = "List of feedbacks on the current page")
    private List<FeedbackResponse> feedbacks;


    @Schema(description = "Total number of items across all pages", example = "100")
    private Integer totalCount;
} 