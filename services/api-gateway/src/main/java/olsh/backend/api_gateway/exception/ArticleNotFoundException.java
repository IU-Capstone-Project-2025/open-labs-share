package olsh.backend.api_gateway.exception;

public class ArticleNotFoundException extends RuntimeException{

    public static final String code = "404_ARTICLE_NOT_FOUND";

    public ArticleNotFoundException(String message) {
        super(message);
    }
}
