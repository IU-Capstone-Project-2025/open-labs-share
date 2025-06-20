package olsh.backend.api_gateway.security.authentication;

import lombok.Getter;
import lombok.Setter;
import olsh.backend.api_gateway.security.model.SecurityUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

@Getter
@Setter
public class TokenAuthentication implements Authentication {

    private String token;
    private SecurityUserDetails userDetails;
    private boolean authenticated = false;

    public TokenAuthentication(String token) {
        this.token = token;
    }

    public TokenAuthentication(String token, SecurityUserDetails userDetails) {
        this.token = token;
        this.userDetails = userDetails;
        this.authenticated = true;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return userDetails != null ? userDetails.getAuthorities() : null;
    }

    @Override
    public Object getCredentials() {
        return token;
    }

    @Override
    public Object getDetails() {
        return userDetails;
    }

    @Override
    public Object getPrincipal() {
        return userDetails;
    }

    @Override
    public String getName() {
        return userDetails != null ? userDetails.getUsername() : null;
    }
}
