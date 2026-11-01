package you.fileserver.dto.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Tolerate;

/**
 * アップロードされたファイルのパスワード用DTO
 */
@Data
@Entity
@Builder
public class UploadFilePassword {
    @Id
    private String fileID; //ファイルID
    private String downloadPassword; //ダウンロードパスワード
    private String deletePassword; //削除パスワード

    @Tolerate //builderを使いたいのでこのアノテーションでディフォルトコンストラクタをLombokに隠ぺいする
    public UploadFilePassword() {}
}
