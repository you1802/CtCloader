package you.fileserver.exception;

public class TargetFileNotFoundException extends RuntimeException {
    public TargetFileNotFoundException(String fileID) {
        super(fileID);
    }
}
