package olsh.backend.marimoservice.exception;

public class ComponentNotFoundException extends RuntimeException {
    public ComponentNotFoundException(String message) {
        super(message);
    }

    public ComponentNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
} 