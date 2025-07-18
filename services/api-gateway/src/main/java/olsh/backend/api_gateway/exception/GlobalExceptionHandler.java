package olsh.backend.api_gateway.exception;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
@ApiResponses(value = {
        @ApiResponse(
                responseCode = "400",
                description = "Bad Request - Invalid input data",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation =
                        ErrorResponse.class))
        ),
        @ApiResponse(
                responseCode = "401",
                description = "Unauthorized - Authentication required",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation =
                        ErrorResponse.class))
        ),
        @ApiResponse(
                responseCode = "403",
                description = "Forbidden - Insufficient permissions",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation =
                        ErrorResponse.class))
        ),
        @ApiResponse(
                responseCode = "404",
                description = "Not Found - Resource not found",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation =
                        ErrorResponse.class))
        ),
        @ApiResponse(
                responseCode = "413",
                description = "Payload Too Large - The uploaded file exceeds the maximum allowed size",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation =
                        ErrorResponse.class))
        ),
        @ApiResponse(
                responseCode = "500",
                description = "Internal Server Error",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation =
                        ErrorResponse.class))
        )
})
public class GlobalExceptionHandler {

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message, String details) {
        return ResponseEntity.status(status).body(
                ErrorResponse.builder()
                        .message(message != null ? message : "")
                        .details(details != null ? details : "")
                        .build()
        );
    }

    @ExceptionHandler(ContentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleContentNotFoundException(ContentNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "The requested content was not found", ex.getMessage());
    }


    @ExceptionHandler(GrpcError.class)
    public ResponseEntity<ErrorResponse> handleGrpcError(GrpcError ex) {
        return buildResponse(ex.getStatus(), ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(TagNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTagNotFoundException(TagNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, TagNotFoundException.class.getSimpleName(), ex.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        return buildResponse(HttpStatus.PAYLOAD_TOO_LARGE,
                "The uploaded file exceeds the maximum allowed size of 100MB",
                "File size exceeds maximum allowed size");
    }

    @ExceptionHandler(SubmissionIsAlreadyGradedException.class)
    public ResponseEntity<ErrorResponse> handleSubmissionIsAlreadyGradedException(SubmissionIsAlreadyGradedException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), "The submission has already been graded");
    }

    @ExceptionHandler(SubmissionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleSubmissionNotFoundException(SubmissionNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), "The requested submission was not found");
    }

    @ExceptionHandler(LabNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleLabNotFoundException(LabNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), "The requested lab was not found");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Invalid arguments were provided", ex.getMessage());
    }

    @ExceptionHandler(ArticleNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleFArticleNotFoundException(ArticleNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), "The requested article was not found");
    }

    @ExceptionHandler(ForbiddenAccessException.class)
    public ResponseEntity<ErrorResponse> handleForbiddenAccessException(ForbiddenAccessException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage(), "");
    }

    @ExceptionHandler(AssetUploadException.class)
    public ResponseEntity<ErrorResponse> handleAssetUploadException(AssetUploadException ex) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), "");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationExceptions(AuthenticationException ex) {
        return buildResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), "");
    }

    // Use for @Valid annotations
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error -> {
            errors.put(error.getField(), error.getDefaultMessage());
        });

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFoundException(UserNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), "The requested user was not found.");
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFoundException(NoHandlerFoundException ex) {
        String method = ex.getHttpMethod();
        String path = ex.getRequestURL();
        String details = String.format("%s %s is not a valid endpoint", method, path);

        return buildResponse(HttpStatus.NOT_FOUND, "Endpoint not found", details);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.", ex.getMessage());
    }

    @ExceptionHandler(FeedbackNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleFeedbackNotFoundException(FeedbackNotFoundException e) {
        return buildResponse(HttpStatus.NOT_FOUND, e.getMessage(), "The requested feedback was not found");
    }
}
