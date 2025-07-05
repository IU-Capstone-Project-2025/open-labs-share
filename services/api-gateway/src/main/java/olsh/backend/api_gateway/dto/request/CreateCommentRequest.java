package olsh.backend.api_gateway.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request object for creating a new comment")
public class CreateCommentRequest {

    @Schema(description = "The content of the comment", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Content cannot be blank")
    private String content;

    @Schema(description = "The ID of the parent comment, if this is a reply")
    private String parentId = "";
} 