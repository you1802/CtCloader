package you.fileserver.dto.projection;

/**
 * ファイルサイズ取得用プロジェクション
 */
public interface FileSizeInfoProjection {
    long getSize();
    String getOwner();
    boolean isVisible();
}
