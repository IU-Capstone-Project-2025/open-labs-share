package olsh.backend.api_gateway.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response object containing feedback details")
public class FeedbackResponse {
    @Schema(description = "Unique identifier of the feedback", example = "c1b2c3d4-e5f6-7890-1234-567890abcdef")
    private String id;

    @Schema(description = "ID of the submission being reviewed", example = "456")
    private Long submissionId;

    @Schema(description = "Student who received the feedback")
    private UserResponse student;

    @Schema(description = "Reviewer who created the feedback")
    private UserResponse reviewer;

    @Schema(description = "Content of the feedback", example = "Great work on implementing the algorithms!")
    private String content;

    @Schema(description = "Creation date and time in ISO 8601 format", example = "2024-03-15T14:30:00Z")
    private String createdAt;

    @Schema(description = "Last update date and time in ISO 8601 format", example = "2024-03-15T14:30:00Z")
    private String updatedAt;

    @Schema(description = "List of attachments associated with the feedback")
    private List<FeedbackAssetResponse> attachments;
} 