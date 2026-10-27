package you.fileserver.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * アップロードされたファイルの詳細情報をユーザーに送るためのDTO
 */
@Builder
@Data
public class FileDetail {
    private String name; //ファイルのもともとの名前
    private long size; //ファイルサイズ
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") //ブラウザにJSONを送るときに見やすい表示に変更する
    private LocalDateTime uploadDate; //アップロード日時
    private boolean owned; //ファイルの所有者かどうか
    private String fileID; //サーバーに保存するファイルの実態の名前(ID)
    private boolean downloadLock; //ダウンロードのパスワードロック
    private String comment; //ファイルのコメント
    private int downloadCount; //ダウンロード数
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") //ブラウザにJSONを送るときに見やすい表示に変更する
    private LocalDateTime lastDownloadDate; //最終ダウンロード日時
}
