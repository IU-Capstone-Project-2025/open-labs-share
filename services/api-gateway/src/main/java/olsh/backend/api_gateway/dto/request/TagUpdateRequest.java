package olsh.backend.api_gateway.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Request to update a tag")
public class TagUpdateRequest {

    @Schema(description = "Tag ID", example = "1", required = true)
    private Long id;

    @Schema(description = "Tag name", example = "Java")
    private String name;

    @Schema(description = "Tag description", example = "Java programming language")
    private String description;
} 