package you.fileserver.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Builder
@Data
public class DownloadBridgePageInfo {
    private String filename; //ファイル名
    private long size; //サイズ
    private String owner; //アップロードしたユーザー名
    private String comment; //コメント
    private LocalDateTime expirationDate; //ダウンロードの有効期限
    private String DownloadUrl; //ダウンロード用URL
}