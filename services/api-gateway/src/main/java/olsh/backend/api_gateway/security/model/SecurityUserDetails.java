package olsh.backend.api_gateway.security.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@RequiredArgsConstructor
public class SecurityUserDetails implements UserDetails {

    @Getter
    private final AuthenticatedUser authenticatedUser;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(
                new SimpleGrantedAuthority(authenticatedUser.getRole())
        );
    }

    @Override
    public String getPassword() {
        return null; // No password needed for token-based auth
    }

    @Override
    public String getUsername() {
        return authenticatedUser.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return System.currentTimeMillis() < authenticatedUser.getExpirationTime() * 1000;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
