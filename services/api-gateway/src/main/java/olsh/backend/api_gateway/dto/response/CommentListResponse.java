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
@Schema(description = "Response object for a paginated list of comments")
public class CommentListResponse {

    @Schema(description = "List of comments on the current page")
    private List<CommentResponse> comments;

    @Schema(description = "Total number of entries")
    private int count;
} 