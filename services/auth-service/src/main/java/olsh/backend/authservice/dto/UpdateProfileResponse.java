package olsh.backend.authservice.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response for profile update operation containing updated user info and tokens")
public class UpdateProfileResponse {

    @Schema(description = "Updated user information")
    private UserInfo userInfo;

    @Schema(description = "JWT access token (only provided if username was changed, null otherwise)")
    private String accessToken;

    @Schema(description = "JWT refresh token (only provided if username was changed, null otherwise)")
    private String refreshToken;

    @Schema(description = "Token expiration time (only provided if username was changed, null otherwise)")
    private LocalDateTime expiresAt;

    @Schema(description = "Token type (only provided if username was changed, null otherwise)", example = "Bearer")
    private String tokenType;

    @Schema(description = "Flag indicating if username was changed and new tokens were issued")
    private boolean usernameChanged;

    @Schema(description = "Success message")
    private String message;
} 