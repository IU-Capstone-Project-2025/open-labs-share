package olsh.backend.authservice.advice;

import java.util.List;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import olsh.backend.authservice.exception.AlreadyExistsException;
import olsh.backend.authservice.exception.BadRequestException;
import olsh.backend.authservice.exception.ErrorResponse;
import olsh.backend.authservice.exception.NotFoundException;
import olsh.backend.authservice.exception.RequestValidationException;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RestExceptionHandler {
    @ExceptionHandler({ IllegalArgumentException.class })
    public ResponseEntity<Object> handleException(IllegalArgumentException e) {
        String errorMessage = "Something in your request is wrong. Details: " + e.getMessage();
        ErrorResponse errorResponse =
            new ErrorResponse(HttpStatus.BAD_REQUEST.value(), errorMessage);
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<Object> handleUnsupportedOperationException(UnsupportedOperationException ex) {
        String errorMessage = "Such operation is unsupported. Details: " + ex.getMessage();
        ErrorResponse errorResponse =
            new ErrorResponse(HttpStatus.BAD_REQUEST.value(), errorMessage);
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(AlreadyExistsException.class)
    public ResponseEntity<Object> handleAlreadyExistsException(AlreadyExistsException ex) {
        String errorMessage = "Something already exists. Details: " + ex.getMessage();
        ErrorResponse errorResponse =
            new ErrorResponse(HttpStatus.BAD_REQUEST.value(), errorMessage);
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Object> handleBadRequestException(BadRequestException ex) {
        String errorMessage = "Something bad and unknown happened. Details: " + ex.getMessage();
        ErrorResponse errorResponse =
            new ErrorResponse(HttpStatus.BAD_REQUEST.value(), errorMessage);
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleMethodArgumentValidationException(
        MethodArgumentNotValidException ex) {
        BindingResult result = ex.getBindingResult();
        List<String> errors = result.getAllErrors()
            .stream()
            .map(DefaultMessageSourceResolvable::getDefaultMessage)
            .collect(Collectors.toList());
        String message =
            "Validation failed. The following errors occurred: " + String.join(", ", errors);
        ErrorResponse errorResponse =
            new ErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY.value(), message);
        return ResponseEntity.unprocessableEntity().body(errorResponse);
    }

    @ExceptionHandler(RequestValidationException.class)
    public ResponseEntity<Object> handleRequestValidationException(RequestValidationException ex) {
        String message = "Validation failed. Constraints: " + ex.getMessage();
        ErrorResponse errorResponse =
            new ErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY.value(), message);
        return ResponseEntity.unprocessableEntity().body(errorResponse);
    }

    @ExceptionHandler(MissingPathVariableException.class)
    public ResponseEntity<Object> handleMissingPathVariableException(MissingPathVariableException ex) {
        log.error("MissingPathVariableException: {}", ex.getMessage());
        String message = "Path variable is required to proceed with the request";
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), message);
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Object> handleBadCredentialsException(BadCredentialsException ex) {
        log.error("BadCredentialsException: {}", ex.getMessage());
        String message = "Bad credentials. Username or password is wrong";
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.UNAUTHORIZED.value(), message);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Object> handleNotFoundException(NotFoundException ex) {
        log.error("NotFoundException: {}", ex.getMessage());
        return ResponseEntity.notFound().build();
    }
}
