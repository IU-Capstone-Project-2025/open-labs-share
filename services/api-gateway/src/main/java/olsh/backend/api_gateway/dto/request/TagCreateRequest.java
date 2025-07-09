package olsh.backend.api_gateway.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Request to create a tag")
public class TagCreateRequest {
    @NotBlank
    @Schema(description = "Tag name", example = "Java", required = true)
    private String name;

    @Schema(description = "Tag description", example = "Java programming language")
    private String description;
} 