package olsh.backend.authservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Request to authorize")
public class SignInRequest {
    @Schema(description = "Username or email", example = "johndoe OR johndoe@mail.com")
    @NotBlank(message = "Username/email must not be blank")
    @Size(min = 2, max = 255, message = "Username/email must contain from 2 to 255 characters")
    private String usernameOrEmail;

    @Schema(description = "Password", example = "secret_password_123")
    @Size(min = 8, max = 255, message = "Password length must contain from 8 to 255 characters")
    private String password;
}
