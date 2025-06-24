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

    @Schema(description = "Pagination information")
    private PaginationResponse pagination;

    @Data
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
