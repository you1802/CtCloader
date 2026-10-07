package you.fileserver.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * アップロードされたファイルの詳細情報をユーザーに送るためのクラス
 */
@Builder
@Data
public class FileDetail {
    private String name;
    private long size;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") //ブラウザにJSONを送るときに見やすい表示に変更する
    private LocalDateTime uploadDate;
    private boolean owned;
    private String uniqueFileName;
    private boolean downloadLock;
    private String comment;
}
