package olsh.backend.api_gateway.exception;

public class AuthenticationException extends RuntimeException{

    public static final String code = "401_UNAUTHENTICATED_REQUEST";

    public AuthenticationException(String message) {
        super(message);
    }
}
