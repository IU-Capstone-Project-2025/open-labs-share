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

    @Schema(description = "User ID extracted from token", example = "123")
    private Long userId;

    @Schema(description = "Username extracted from token", example = "johndoe")
    private String username;
    
    @Schema(description = "First name extracted from token", example = "John")
    private String firstName;
    
    @Schema(description = "Last name extracted from token", example = "Doe")
    private String lastName;

    @Schema(description = "User role extracted from token", example = "USER")
    private String role;

    @Schema(description = "Token expiration timestamp", example = "1622506800")
    private Long expirationTime;

    @Schema(description = "Error message if token is invalid", example = "Token has expired")
    private String errorMessage;
}
