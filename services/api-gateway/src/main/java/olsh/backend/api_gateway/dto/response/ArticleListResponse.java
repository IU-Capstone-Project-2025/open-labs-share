package olsh.backend.api_gateway.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArticleListResponse {
    private List<ArticleResponse> articles;
    private PaginationResponse pagination;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PaginationResponse {
        private Integer currentPage;
        private Integer totalPages;
        private Integer totalItems;
    }
}

