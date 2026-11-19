package you.fileserver.dto.projection;

public interface DownloadBridgePageInfoProjection {
    String getFilename();
    long getSize();
    String getOwner();
    String getComment();
}
