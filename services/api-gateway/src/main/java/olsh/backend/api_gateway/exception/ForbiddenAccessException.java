package olsh.backend.api_gateway.exception;

public class ForbiddenAccessException extends RuntimeException {
    public static final String code = "403_YOU_HAVE_NO_PERMISSION";

    public ForbiddenAccessException(String message) {
        super(message);
    }
}
