package you.fileserver.dto.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Tolerate;

import java.time.LocalDateTime;

/**
 * ダウンロード用トークンのDTO
 */
@Data
@Entity
@Builder
public class DownloadTokenInfo {
    @Id
    private String token; //ダウンロードに必要な一時トークン
    private String fileID; //ダウンロードするファイルID
    private LocalDateTime expirationDate; //有効期限

    @Tolerate
    public DownloadTokenInfo() {}
}
