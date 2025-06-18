package olsh.backend.authservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Request to authorize")
public class SignInRequest {
    @Schema(description = "Username", example = "John Doe")
    @NotBlank(message = "Username must not be blank")
    @Size(min = 2, max = 50, message = "Username must contain from 2 to 50 characters")
    private String username;

    @Schema(description = "Password", example = "secret_password_123")
    @Size(min = 8, max = 255, message = "Password length must contain from 8 to 255 characters")
    private String password;
}
