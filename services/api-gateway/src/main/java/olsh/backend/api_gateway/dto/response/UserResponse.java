package olsh.backend.api_gateway.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response object containing user details")
public class UserResponse {
    @Schema(description = "Unique identifier of the user", example = "1")
    Long id;

    @Schema(description = "Username of the user", example = "johndoe")
    String username;

    @Schema(description = "First name of the user", example = "John")
    String name;

    @Schema(description = "Last name of the user", example = "Doe")
    String surname;

    @Schema(description = "Email address of the user", example = "john.doe@example.com")
    String email;

    @Schema(description = "Number of submissions", example = "7")
    Integer labs_solved;

    @Schema(description = "Number of reviewed submissions", example = "8")
    Integer labs_reviewed;

    @Schema(description = "Current points balance", example = "9")
    Integer balance;
}
