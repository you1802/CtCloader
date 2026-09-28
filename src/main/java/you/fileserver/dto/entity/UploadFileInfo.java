package you.fileserver.dto.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Tolerate;

import java.time.LocalDateTime;

/**
 * リポジトリ保存用のアップロードされたファイル情報用DTO
 */
@Data
@Entity
@Builder
public class UploadFileInfo {
    @Id
    private String uniqueFileName;
    private String owner;
    private String originalFileName;
    private long size;
    private LocalDateTime uploadDate;
    private boolean visible;
    private boolean downloadLocked;

    @Tolerate //builderを使いたいのでこのアノテーションでディフォルトコンストラクタをLombokに隠ぺいする
    public UploadFileInfo() {}
}
