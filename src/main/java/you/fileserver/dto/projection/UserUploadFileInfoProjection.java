package you.fileserver.dto.projection;

import java.time.LocalDateTime;

/**
 * ユーザー用のファイルリスト情報のプロジェクション
 */
public interface UserUploadFileInfoProjection {
    String getUniqueFileName();
    String getOwner();
    String getOriginalFileName();
    long getSize();
    LocalDateTime getUploadDate();
    boolean isDownloadLocked();
    String getComment();
    int getDownloadCount();
    LocalDateTime getLastDownloadDate();
}
