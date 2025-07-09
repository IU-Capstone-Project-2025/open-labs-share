package olsh.backend.api_gateway.exception;

public class TagNotFoundException extends RuntimeException{
    public TagNotFoundException(long tagId) {
        super("Tag with ID " + tagId + " not found");
    }

    public TagNotFoundException(String message) {
        super(message);
    }
}
