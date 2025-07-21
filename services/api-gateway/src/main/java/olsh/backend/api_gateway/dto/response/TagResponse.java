package olsh.backend.api_gateway.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Response with tag information")
public class TagResponse {

    @Schema(description = "Tag ID", example = "1")
    private int id;

    @Schema(description = "Tag name", example = "Java")
    private String name;

    @Schema(description = "Tag description", example = "Java programming language")
    private String description;

    @Schema(description = "Number of labs with this tag", example = "5")
    private int labs_count;
}
