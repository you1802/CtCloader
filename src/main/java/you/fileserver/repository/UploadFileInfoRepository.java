package you.fileserver.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import you.fileserver.dto.entity.UploadFileInfo;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UploadFileInfoRepository extends JpaRepository<UploadFileInfo,String> {
    /**
     * ログイン中のユーザーのファイルリストを作成するための情報を引き出すジェネリックメソッド
     * @param owner ログイン中のユーザー名
     * @param visible ファイルの可視化設定
     * @param type プロジェクションクラス
     * @return 必要な情報だけ取り出したリスト
     * @param <T> プロジェクションクラス
     */
    <T> List<T> findByOwnerOrVisible(String owner, boolean visible, Class<T> type);

    /**
     * 非ログイン中ユーザーのファイルリストを作成するための情報を引き出す
     * @param visible ファイルの可視化設定
     * @param type プロジェクションクラス
     * @return 必要な情報だけ取り出したリスト
     * @param <T> プロジェクションクラス
     */
    <T> List<T> findByVisible(boolean visible, Class<T> type);

    /**
     * DB全体から必要な情報を引き出す
     * @param type プロジェクションクラス
     * @return 必要な情報のリスト
     * @param <T> プロジェクションクラス
     */
    <T> List<T> findAllBy(Class<T> type);

//    /**
//     *
//     * @param uniqueFileName
//     * @param type
//     * @return
//     * @param <T>
//     */
//    <T> Optional<T> findById(String uniqueFileName, Class<T> type);

    /**
     * ダウンロード数を＋1し最終ダウンロード日時を更新するメソッド
     * @param fileID 対象ファイルID
     * @param now 現在時刻
     */
    @Transactional
    @Modifying
    @Query("UPDATE UploadFileInfo ufi SET ufi.downloadCount = ufi.downloadCount + 1, ufi.lastDownloadDate = :now WHERE ufi.fileID = :fileID")
    void updateDownloadCountAndDate(@Param("fileID") String fileID, @Param("now") LocalDateTime now);
}
