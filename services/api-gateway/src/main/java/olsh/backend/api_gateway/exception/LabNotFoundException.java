package olsh.backend.api_gateway.exception;

public class LabNotFoundException extends ContentNotFoundException {
    public LabNotFoundException(String message) {
        super(message);
    }
}

