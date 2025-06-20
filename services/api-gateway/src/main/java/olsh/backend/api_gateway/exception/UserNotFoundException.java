package olsh.backend.api_gateway.exception;

public class UserNotFoundException extends RuntimeException{

    public static final String code = "404_USER_NOT_FOUND";

    public UserNotFoundException(String message) {
        super(message);
    }
}
