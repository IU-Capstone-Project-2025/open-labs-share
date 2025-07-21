package olsh.backend.api_gateway.dto.response;

import lombok.Builder;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Builder
@Schema(description = "Response object returned after successful laboratory work deletion")
public class LabDeleteResponse {
    @Schema(description = "Success message", example = "Lab deleted successfully")
    private String message;
}

