package you.fileserver.dto.projection;

import java.time.LocalDateTime;

/**
 * ユーザー用のファイルリスト情報のプロジェクション
 */
public interface UserUploadFileInfoProjection {
    String getFileID();
    String getOwner();
    String getFilename();
    long getSize();
    LocalDateTime getUploadDate();
    boolean isDownloadLocked();
    String getComment();
    int getDownloadCount();
    LocalDateTime getLastDownloadDate();
}
