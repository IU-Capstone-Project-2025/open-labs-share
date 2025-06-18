package olsh.backend.authservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Request to reset password")
public class PasswordResetRequest {
    @Schema(description = "Email address", example = "johndoe@mail.com")
    @Size(min = 5, max = 255, message = "Email must contain from 5 to 255 characters")
    @NotBlank(message = "Email must not be blank")
    @Email(message = "Email must be in format user@example.com")
    private String email;
}
