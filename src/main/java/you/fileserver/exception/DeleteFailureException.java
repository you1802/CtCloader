package you.fileserver.exception;

public class DeleteFailureException extends RuntimeException {
    public DeleteFailureException(String message) {
        super(message);
    }
}
