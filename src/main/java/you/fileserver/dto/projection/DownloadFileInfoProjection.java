package you.fileserver.dto.projection;

/**
 * ダウンロード時に必要な情報だけ取得するためのプロジェクション
 */
public interface DownloadFileInfoProjection {
    String getOwner();
    String getOriginalFileName();
    boolean isVisible();
    boolean isDownloadLocked();
}
