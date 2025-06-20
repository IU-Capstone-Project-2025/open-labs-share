package olsh.backend.api_gateway.security.authentication;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import olsh.backend.api_gateway.grpc.model.AuthValidationResponse;
import olsh.backend.api_gateway.security.model.AuthenticatedUser;
import olsh.backend.api_gateway.security.model.SecurityUserDetails;
import olsh.backend.api_gateway.service.AuthService;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenAuthenticationProvider implements AuthenticationProvider {

    private final AuthService authService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        TokenAuthentication tokenAuth = (TokenAuthentication) authentication;
        String token = tokenAuth.getToken();

        try {
            AuthValidationResponse response = authService.validateToken(token);

            SecurityUserDetails userDetails = getSecurityUserDetails(response);
            return new TokenAuthentication(token, userDetails);
        } catch (Exception e) {
            log.error("Authentication failed for token", e);
            throw new org.springframework.security.authentication.BadCredentialsException("Authentication failed", e);
        }
    }

    private static SecurityUserDetails getSecurityUserDetails(AuthValidationResponse response) {
        if (!response.isValid()) {
            throw new org.springframework.security.authentication.BadCredentialsException(
                    response.getErrorMessage() != null ? response.getErrorMessage() : "Invalid token"
            );
        }

        AuthenticatedUser user = new AuthenticatedUser(
                response.getUserInfo().getId(),
                response.getUserInfo().getUsername(),
                response.getUserInfo().getFirstName(),
                response.getUserInfo().getLastName(),
                response.getUserInfo().getRole(),
                response.getExpirationTime()
        );

        SecurityUserDetails userDetails = new SecurityUserDetails(user);
        return userDetails;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return TokenAuthentication.class.isAssignableFrom(authentication);
    }
}
