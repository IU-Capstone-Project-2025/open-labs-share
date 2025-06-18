package olsh.backend.authservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Request to confirm password reset")
public class PasswordResetConfirmRequest {
    @Schema(description = "Password reset token", example = "abc123xyz")
    @NotBlank(message = "Reset token must not be blank")
    private String token;

    @Schema(description = "New password", example = "new_secret_password_123")
    @Size(min = 8, max = 255, message = "Password length must contain from 8 to 255 characters")
    @NotBlank(message = "Password must not be blank")
    private String newPassword;
}
