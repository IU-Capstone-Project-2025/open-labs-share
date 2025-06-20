package olsh.backend.api_gateway.exception;

public class UserAlreadyExistsException extends RuntimeException{

    public static final String code = "409_USER_ALREADY_EXISTS";

    public UserAlreadyExistsException(String message) {
        super(message);
    }
}
