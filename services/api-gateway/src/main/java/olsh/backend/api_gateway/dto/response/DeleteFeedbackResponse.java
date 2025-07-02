package olsh.backend.api_gateway.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response object returned after successful feedback deletion")
public class DeleteFeedbackResponse {

    @Schema(description = "Operation result", example = "true")
    private boolean result;

    @Schema(description = "Success message", example = "Feedback deleted successfully")
    private String message;
} 