package olsh.backend.api_gateway.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import olsh.backend.api_gateway.exception.AuthenticationException;
import olsh.backend.api_gateway.grpc.client.AuthServiceClient;
import olsh.backend.api_gateway.grpc.model.AuthValidationResponse;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthServiceClient authServiceClient;

    /**
     * Validates a JWT token by sending it to the gRPC auth service. Throws if the token is missing or invalid.
     */
    public AuthValidationResponse validateToken(String token) {
        if (token == null || token.isBlank()) {
            throw new AuthenticationException("Token is required");
        }

        return authServiceClient.validateToken(token);
    }
}
