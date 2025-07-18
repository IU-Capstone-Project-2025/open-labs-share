package olsh.backend.api_gateway.exception;

public class ContentNotFoundException extends RuntimeException{

    public ContentNotFoundException(String message){
        super(message);
    }
}
