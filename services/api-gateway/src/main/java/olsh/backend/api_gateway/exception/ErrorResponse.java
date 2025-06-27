package olsh.backend.api_gateway.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "Standard error response object")
public class ErrorResponse {

    @Schema(description = "Error message", example = "Resource not found")
    private String message;

    @Schema(description = "Detailed error information", example = "Article with ID 123 was not found")
    private String details;
}
