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
@Schema(description = "Response object returned after successful article deletion")
public class DeleteArticleResponse {
    @Schema(description = "Success message", example = "Article deleted successfully")
    private String message;
}

