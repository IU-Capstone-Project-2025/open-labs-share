package olsh.backend.marimomanagerservice.utils;

import olsh.backend.grpc.auth.ValidateTokenResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.stream.Collectors;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static UserDetails buildUserDetails(ValidateTokenResponse validationResponse) {
        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + validationResponse.getUserInfo().getRole())
        );

        return new User(
                validationResponse.getUserInfo().getUsername(),
                "", // Password is not needed as we use token-based auth
                authorities
        );
    }
} 