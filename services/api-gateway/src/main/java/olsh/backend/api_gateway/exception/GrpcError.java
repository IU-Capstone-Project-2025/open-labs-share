package olsh.backend.api_gateway.exception;

import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
public class GrpcError extends RuntimeException{

    private String message;
    private String code;
    private HttpStatus status;

    public GrpcError(HttpStatus status, String message, String code) {
        super(message);
        this.status = status;
        this.message = message;
        this.code = code;
    }
}
