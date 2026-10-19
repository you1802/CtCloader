package you.fileserver.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import you.fileserver.dto.entity.UploadFileInfo;
import you.fileserver.dto.projection.UserUploadFileInfoProjection;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UploadFileInfoRepository extends JpaRepository<UploadFileInfo,String> {
    /**
     * ログイン中のユーザーのファイルリストを作成するための情報を引き出す
     * @param owner ログイン中のユーザー名
     * @param visible ファイルの可視化設定
     * @return 必要な情報だけ取り出したリスト
     */
    List<UserUploadFileInfoProjection> findByOwnerOrVisible(String owner, boolean visible);

    /**
     * 非ログイン中ユーザーのファイルリストを作成するための情報を引き出す
     * @param visible ファイルの可視化設定
     * @return 必要な情報だけ取り出したリスト
     */
    List<UserUploadFileInfoProjection> findByVisible(boolean visible);

    /**
     * ダウンロード数を＋1し最終ダウンロード日時を更新するメソッド
     * @param uniqueFileName 対象ファイル名
     * @param now 現在時刻
     */
    @Transactional
    @Modifying
    @Query("UPDATE UploadFileInfo ufi SET ufi.downloadCount = ufi.downloadCount + 1, ufi.lastDownloadDate = :now WHERE ufi.uniqueFileName = :uniqueFileName")
    void updateDownloadCountAndDate(@Param("uniqueFileName") String uniqueFileName, @Param("now") LocalDateTime now);
}
