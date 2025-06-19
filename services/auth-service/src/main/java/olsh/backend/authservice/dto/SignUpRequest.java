package olsh.backend.authservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Request to register")
public class SignUpRequest {
    @Schema(description = "Username", example = "John Doe")
    @Size(min = 2, max = 50, message = "Username must contain from 2 to 50 characters")
    @NotBlank(message = "Username must not be blank")
    private String username;

    @Schema(description = "First name", example = "John")
    @Size(min = 2, max = 50, message = "First name must contain from 2 to 50 characters")
    @NotBlank(message = "First name must not be blank")
    private String firstName;

    @Schema(description = "Last name", example = "Doe")
    @Size(min = 2, max = 50, message = "Last name must contain from 2 to 50 characters")
    @NotBlank(message = "Last name must not be blank")
    private String lastName;

    @Schema(description = "Email address", example = "johndoe@mail.com")
    @Size(min = 5, max = 255, message = "Email must contain from 5 to 255 characters")
    @NotBlank(message = "Email must not be blank")
    @Email(message = "Email must be in format user@example.com")
    private String email;

    @Schema(description = "Password", example = "secret_password_123")
    @Size(min = 8, max = 255, message = "Password length must contain from 8 to 255 characters")
    private String password;
}

