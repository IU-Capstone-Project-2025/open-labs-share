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
@Schema(description = "Basic user info for responses")
public class UserInfo {
    @Schema(description = "User ID", example = "123")
    private Long userId;

    @Schema(description = "Username", example = "johndoe")
    private String username;

    @Schema(description = "First name", example = "John")
    private String firstName;

    @Schema(description = "Last name", example = "Doe")
    private String lastName;

    @Schema(description = "User role", example = "USER")
    private String role;
    
    @Schema(description = "Email address", example = "john.doe@example.com")
    private String email;

    @Schema(description = "Number of labs solved by the user", example = "5")
    private Integer labsSolved;

    @Schema(description = "Number of labs reviewed by the user", example = "3")
    private Integer labsReviewed;

    @Schema(description = "User's current points balance", example = "40")
    private Integer balance;
}
