package olsh.backend.api_gateway.exception;

public class LabNotFoundException extends RuntimeException {
    public LabNotFoundException(String message) {
        super(message);
    }
}

