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
@Schema(description = "User profile information")
public class UserProfileResponse {
    @Schema(description = "User ID", example = "123")
    private Long id;

    @Schema(description = "Username", example = "johndoe")
    private String username;

    @Schema(description = "First name", example = "John")
    private String firstName;

    @Schema(description = "Last name", example = "Doe")
    private String lastName;

    @Schema(description = "Email address", example = "johndoe@mail.com")
    private String email;

    @Schema(description = "User role", example = "USER")
    private String role;

    @Schema(description = "Account creation timestamp", example = "2023-01-15T10:30:00")
    private String createdAt;

    @Schema(description = "Last login timestamp", example = "2023-01-20T14:45:00")
    private String lastLoginAt;

    @Schema(description = "Account status", example = "ACTIVE")
    private String status;
}
