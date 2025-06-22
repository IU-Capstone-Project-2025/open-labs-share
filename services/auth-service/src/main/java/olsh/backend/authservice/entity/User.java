package olsh.backend.authservice.entity;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Non-persistent User model for authentication purposes only.
 * All user data is retrieved from users-service via gRPC.
 * This class serves only as a container for JWT token generation and Spring Security integration.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {

    private Long userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String password;
    private Role role;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }
}
