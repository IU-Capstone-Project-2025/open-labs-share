package olsh.backend.api_gateway.exception;

public class SubmissionIsAlreadyGradedException extends RuntimeException{
    public SubmissionIsAlreadyGradedException() {
        super("Submission is already graded");
    }

    public SubmissionIsAlreadyGradedException(String message) {
        super(message);
    }

    public SubmissionIsAlreadyGradedException(String message, Throwable cause) {
        super(message, cause);
    }

    public SubmissionIsAlreadyGradedException(Throwable cause) {
        super(cause);
    }
}
