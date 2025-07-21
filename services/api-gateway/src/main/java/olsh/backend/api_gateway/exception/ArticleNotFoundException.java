package olsh.backend.api_gateway.exception;

public class ArticleNotFoundException extends ContentNotFoundException{

    public ArticleNotFoundException(String message) {
        super(message);
    }
}
