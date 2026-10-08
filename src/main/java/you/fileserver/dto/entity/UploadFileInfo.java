package you.fileserver.dto.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Tolerate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * リポジトリ保存用のアップロードされたファイル情報用DTO
 */
@Data
@Entity
@Builder
public class UploadFileInfo {
    @Id
    private String uniqueFileName; //サーバーに保存するファイルの実態の名前
    private String owner; //アップロード時のユーザーID
    private String originalFileName; //ファイルのもともとの名前
    private long size; //ファイルサイズ
    private LocalDateTime uploadDate; //アップロード日時
    private boolean visible; //可視化設定
    private boolean downloadLocked; //ダウンロードのパスワードロック
    private String comment; //ファイルのコメント
    private String ipAddress; //アップロード者のIP

    @OneToMany(mappedBy = "uniqueFileName", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<FileReport> fileReports = new ArrayList<>(); //ファイルの問題報告のリスト

    @Tolerate //builderを使いたいのでこのアノテーションでディフォルトコンストラクタをLombokに隠ぺいする
    public UploadFileInfo() {}
}
