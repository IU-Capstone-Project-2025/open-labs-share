package olsh.backend.api_gateway.security.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticatedUser {
    private Long userId;
    private String username;
    private String firstName;
    private String lastName;
    private String role;
    private Long expirationTime;
}