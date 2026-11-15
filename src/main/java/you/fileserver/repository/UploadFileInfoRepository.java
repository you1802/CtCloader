package you.fileserver.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import you.fileserver.dto.AdminForumFileDetail;
import you.fileserver.dto.entity.UploadFileInfo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UploadFileInfoRepository extends JpaRepository<UploadFileInfo,String> {
    /**
     * ログイン中のユーザーの掲示板ファイルリストを作成するための情報を引き出す
     * @param owner ログイン中のユーザー名
     * @param visible ファイルの可視化設定
     * @param type プロジェクションクラス
     * @return 必要な情報だけ取り出したリスト
     * @param <T> ジェネリックメソッド
     */
    <T> List<T> findByOwnerOrVisible(String owner, boolean visible, Class<T> type);

    /**
     * ログイン中ユーザー自身がアップロードしたファイルリストを返す
     * @param owner ログイン中のユーザ名
     * @param type プロジェクションクラス
     * @return 必要な情報だけ取り出したリスト
     * @param <T> ジェネリックメソッド
     */
    <T> List<T> findByOwner(String owner, Class<T> type);

    /**
     * 非ログイン中ユーザーのファイルリストを作成するための情報を引き出す
     * @param visible ファイルの可視化設定
     * @param type プロジェクションクラス
     * @return 必要な情報だけ取り出したリスト
     * @param <T> ジェネリックメソッド
     */
    <T> List<T> findByVisible(boolean visible, Class<T> type);

    /**
     * DB全体から必要な情報を引き出す
     * @param type プロジェクションクラス
     * @return 必要な情報のリスト
     * @param <T> ジェネリックメソッド
     */
    <T> List<T> findAllBy(Class<T> type);

    /**
     * プロジェクション用findById
     * @param fileID ファイル id
     * @param type プロジェクションクラス
     * @return 必要な情報
     * @param <T> ジェネリックメソッド
     */
    @EntityGraph(attributePaths = {"fileReports"}) //fileReportsを取得するときは遅延読み込みを即時読み込みにする
    <T> Optional<T> findByFileID(String fileID, Class<T> type);

    /**
     * ADMIN専用、ファイルデータすべてと問題報告件数を取得する
     * @return リスト
     */
    @Query("""
                SELECT new you.fileserver.dto.AdminForumFileDetail(
                    f.fileID,
                    f.filename,
                    f.size,
                    f.uploadDate,
                    f.lastDownloadDate,
                    f.owner,
                    f.downloadLocked,
                    SIZE(f.fileReports)
                )
                FROM UploadFileInfo f
            """)
    List<AdminForumFileDetail> adminFindAllForumFiles();

    /**
     * ダウンロード数を＋1し最終ダウンロード日時を更新する
     * @param fileID 対象ファイルID
     * @param now 現在時刻
     */
    @Transactional
    @Modifying
    @Query("UPDATE UploadFileInfo ufi SET ufi.downloadCount = ufi.downloadCount + 1, ufi.lastDownloadDate = :now WHERE ufi.fileID = :fileID")
    void updateDownloadCountAndDate(@Param("fileID") String fileID, @Param("now") LocalDateTime now);
}
