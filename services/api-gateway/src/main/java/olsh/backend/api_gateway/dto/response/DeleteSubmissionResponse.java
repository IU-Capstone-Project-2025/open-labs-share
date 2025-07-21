package olsh.backend.api_gateway.dto.response;

import lombok.Builder;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Builder
@Schema(description = "Response object returned after submission deletion attempt")
public class DeleteSubmissionResponse {

    @Schema(description = "Whether the deletion was successful", example = "true")
    private boolean success;

    @Schema(description = "Response message", example = "Submission deleted successfully")
    private String message;
}

