package you.fileserver.dto.projection;

import java.time.LocalDateTime;

/**
 * 掲示板のファイルリスト用プロジェクション
 */
public interface UserForumFileInfoProjection {
    String getFileID();
    String getFilename();
    long getSize();
    LocalDateTime getUploadDate();
    LocalDateTime getLastDownloadDate();
    String getOwner();
    boolean isDownloadLocked();
}
