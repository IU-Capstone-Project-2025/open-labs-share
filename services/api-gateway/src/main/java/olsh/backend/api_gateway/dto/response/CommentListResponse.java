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

    @Schema(description = "Pagination information")
    private PaginationResponse pagination;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Pagination information for list responses")
    public static class PaginationResponse {

        @Schema(description = "Current page number", example = "1")
        private Integer currentPage;

        @Schema(description = "Total number of pages", example = "5")
        private Integer totalPages;

        @Schema(description = "Total number of items across all pages", example = "100")
        private Integer totalItems;
    }
} 