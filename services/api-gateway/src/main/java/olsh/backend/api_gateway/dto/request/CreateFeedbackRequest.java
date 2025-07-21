package olsh.backend.api_gateway.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request object for creating a new feedback")
public class CreateFeedbackRequest {

    @Schema(description = "ID of the student receiving the feedback", example = "123", required = true)
    @NotNull(message = "Student ID is required")
    private Long studentId;

    @Schema(description = "ID of the submission being reviewed", example = "456", required = true)
    @NotNull(message = "Submission ID is required")
    private Long submissionId;

    @Schema(description = "Content of the feedback", example = "Great work on implementing the algorithms!", required = true)
    @NotBlank(message = "Content is required")
    private String content;

    @Schema(description = "Optional files attached to the feedback")
    private MultipartFile[] files;
} 