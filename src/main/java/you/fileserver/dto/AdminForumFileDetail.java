package you.fileserver.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class AdminForumFileDetail {
    private String fileID; //ファイルid
    private String filename; //ファイルのもともとの名前
    private long size; //ファイルサイズ
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") //ブラウザにJSONを送るときに見やすい表示に変更する
    private LocalDateTime uploadDate; //アップロード日時
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") //ブラウザにJSONを送るときに見やすい表示に変更する
    private LocalDateTime lastDownloadDate; //最終ダウンロード日時
    private String owner; //アップロードしたユーザー名
    private boolean downloadLocked; //ダウンロードのパスワードロック
    private int reportCount; //問題報告件数
}
