package you.fileserver.dto.projection;

/**
 * ファイルの詳細情報用プロジェクション
 */
public interface TargetFileDetailProjection {
    String getOwner();
    boolean isVisible();
    String getComment();
    int getDownloadCount();
}
