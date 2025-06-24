package olsh.backend.api_gateway.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class LabListResponse {
    private List<LabResponse> labs;
    private PaginationResponse pagination;

    @Data
    @Builder
    public static class PaginationResponse {
        private Integer currentPage;
        private Integer totalPages;
        private Integer totalItems;
    }
}
