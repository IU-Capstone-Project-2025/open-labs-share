package olsh.backend.api_gateway.dto.response;

import lombok.Builder;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Builder
@Schema(description = "Response object returned after successful submission creation")
public class CreateSubmissionResponse {

    @Schema(description = "Result of the operation, true of false", example = "1")
    private boolean success;

    @Schema(description = "Success message", example = "Submission created successfully")
    private String message;

    @Schema(description = "Submission metadata")
    private SubmissionResponse submissionMetadata;
}

