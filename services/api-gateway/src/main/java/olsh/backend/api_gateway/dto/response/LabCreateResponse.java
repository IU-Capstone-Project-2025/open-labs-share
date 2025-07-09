package olsh.backend.api_gateway.dto.response;

import lombok.Builder;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Builder
@Schema(description = "Response object returned after successful laboratory work creation")
public class LabCreateResponse {
    @Schema(description = "Unique identifier of the created lab", example = "1")
    private Long id;

    @Schema(description = "Success message", example = "Lab created successfully")
    private String message;
}
