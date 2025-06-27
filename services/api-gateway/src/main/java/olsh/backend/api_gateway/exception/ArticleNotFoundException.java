package olsh.backend.api_gateway.exception;

public class ArticleNotFoundException extends RuntimeException{

    public ArticleNotFoundException(String message) {
        super(message);
    }
}
