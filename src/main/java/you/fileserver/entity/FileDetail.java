package you.fileserver.entity;

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
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") //ブラウザに情報を送るときに見やすい表示に変更する
    private LocalDateTime uploadDate;
    private boolean owned;
    private String uniqueFileName;
}
