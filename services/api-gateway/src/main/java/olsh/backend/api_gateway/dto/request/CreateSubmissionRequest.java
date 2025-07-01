package olsh.backend.api_gateway.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Request object for creating a new submission")
public class CreateSubmissionRequest {

    @Schema(description = "ID of the lab this submission belongs to", example = "1", required = true)
    @NotNull(message = "Lab ID is required")
    @Positive(message = "Lab ID must be positive")
    private Long labId;

    @Schema(description = "Text comment for the submission", example = "Here is my solution for the lab work", required = false)
    @Size(max = 5000, message = "Text comment must not exceed 5000 characters")
    private String textComment;

    @Schema(description = "Files attached to the submission", required = false)
    private MultipartFile[] files;
}

