package olsh.backend.api_gateway.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Response object returned after successful article creation")
public class CreateArticleResponse {
    @Schema(description = "Unique identifier of the created article", example = "1")
    private Long id;

    @Schema(description = "Success message", example = "Article created successfully")
    private String message;

    @Schema(description = "Details of the created article")
    private ArticleResponse article;
}

