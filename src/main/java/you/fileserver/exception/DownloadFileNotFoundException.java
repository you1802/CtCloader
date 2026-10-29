package you.fileserver.exception;

public class DownloadFileNotFoundException extends RuntimeException {
    public DownloadFileNotFoundException(String fileID) {
        super(fileID);
    }
}
