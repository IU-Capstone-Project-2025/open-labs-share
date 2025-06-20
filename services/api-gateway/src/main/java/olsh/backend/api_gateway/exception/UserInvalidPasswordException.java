package olsh.backend.api_gateway.exception;

public class UserInvalidPasswordException extends RuntimeException{
    public static final String code = "USER_INVALID_PASSWORD";

    public UserInvalidPasswordException(String message) {
        super(message);
    }

}
