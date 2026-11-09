package you.fileserver.dto.projection;

/**
 * ファイルの削除用プロジェクション
 */
public interface DeleteFileInfoProjection {
    String getFileID();
    String getOwner();
}
