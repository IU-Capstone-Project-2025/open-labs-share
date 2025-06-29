package olsh.backend.authservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request object for updating user profile")
public class UpdateProfileRequest {

    @Schema(description = "First name of the user", example = "John")
    @Size(max = 50, message = "First name must not exceed 50 characters")
    private String firstName;

    @Schema(description = "Last name of the user", example = "Doe")
    @Size(max = 50, message = "Last name must not exceed 50 characters")
    private String lastName;

    @Schema(description = "Username of the user", example = "johndoe")
    @Pattern(regexp = "^[a-zA-Z0-9_]{3,20}$", message = "Username must be 3-20 characters long and contain only letters, numbers, and underscores")
    private String username;

    @Schema(description = "Email address of the user", example = "john.doe@example.com")
    @Email(message = "Email must be a valid email address")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    @Schema(description = "New password (optional, leave blank to keep current password)", example = "newSecurePassword123")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    private String password;
} 