package olsh.backend.authservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Token validation response")
public class TokenValidationResponse {
    @Schema(description = "Whether token is valid", example = "true")
    private boolean valid;

    @Schema(description = "User information extracted from token")
    private UserInfo userInfo;

    @Schema(description = "Token expiration timestamp", example = "1622506800")
    private Long expirationTime;

    @Schema(description = "Error message if token is invalid", example = "Token has expired")
    private String errorMessage;
}
