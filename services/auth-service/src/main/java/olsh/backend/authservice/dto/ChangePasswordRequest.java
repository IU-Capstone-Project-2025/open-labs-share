package olsh.backend.authservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Request to change password")
public class ChangePasswordRequest {
    @Schema(description = "Current password", example = "current_password")
    @NotBlank(message = "Current password must not be blank")
    private String currentPassword;

    @Schema(description = "New password", example = "new_secret_password_123")
    @Size(min = 8, max = 255, message = "Password length must contain from 8 to 255 characters")
    @NotBlank(message = "New password must not be blank")
    private String newPassword;
}
