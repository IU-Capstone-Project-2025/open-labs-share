package olsh.backend.api_gateway.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request object for updating an existing comment")
public class UpdateCommentRequest {

    @Schema(description = "The new content of the comment", required = true)
    @NotBlank(message = "Content cannot be blank")
    private String content;
} 