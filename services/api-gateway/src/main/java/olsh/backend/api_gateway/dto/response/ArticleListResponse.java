package olsh.backend.api_gateway.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Response object containing a paginated list of articles")
public class ArticleListResponse {
    @Schema(description = "List of articles on the current page")
    private List<ArticleResponse> articles;

    @Schema(description = "Pagination information")
    private PaginationResponse pagination;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Pagination details")
    public static class PaginationResponse {
        @Schema(description = "Current page number", example = "1")
        private Integer currentPage;

        @Schema(description = "Total number of pages", example = "5")
        private Integer totalPages;

        @Schema(description = "Total number of items across all pages", example = "100")
        private Integer totalItems;
    }
}

