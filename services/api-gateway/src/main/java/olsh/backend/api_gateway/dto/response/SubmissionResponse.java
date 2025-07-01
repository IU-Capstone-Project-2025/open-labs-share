package olsh.backend.api_gateway.dto.response;

import lombok.Builder;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Data
@Builder
@Schema(description = "Response object containing submission details with associated assets")
public class SubmissionResponse {

    @Schema(description = "Unique identifier of the submission", example = "1")
    private Long submissionId;

    @Schema(description = "ID of the lab this submission belongs to", example = "1")
    private Long labId;

    @Schema(description = "ID of the submission owner", example = "123")
    private Long ownerId;

    @Schema(description = "Owner's username", example = "CoolJo")
    private String username;

    @Schema(description = "Owner's first name", example = "John")
    private String ownerName;

    @Schema(description = "Owner's last name", example = "Doe")
    private String ownerSurname;

    @Schema(description = "Text comment of the submission", example = "Here is my solution for the lab work")
    private String text;

    @Schema(description = "Submission creation date in ISO 8601 format", example = "2024-03-15T14:30:00Z")
    private String createdAt;

    @Schema(description = "Submission last update date in ISO 8601 format", example = "2024-03-15T14:30:00Z")
    private String updatedAt;

    @Schema(description = "Current status of the submission", example = "NOT_GRADED")
    private String status;

    @Schema(description = "List of assets associated with this submission")
    private List<SubmissionAssetResponse> assets;
}
