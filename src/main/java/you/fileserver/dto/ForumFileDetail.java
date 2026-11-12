package you.fileserver.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 掲示板のファイルリスト用DTO
 */
@Data
@Builder
public class ForumFileDetail {
    private String fileID; //ファイルid
    private String filename; //ファイルのもともとの名前
    private long size; //ファイルサイズ
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") //ブラウザにJSONを送るときに見やすい表示に変更する
    private LocalDateTime uploadDate; //アップロード日時
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") //ブラウザにJSONを送るときに見やすい表示に変更する
    private LocalDateTime lastDownloadDate; //最終ダウンロード日時
    private boolean owned; //ファイルの所有者かどうか
    private boolean downloadLocked; //ダウンロードのパスワードロック
}
