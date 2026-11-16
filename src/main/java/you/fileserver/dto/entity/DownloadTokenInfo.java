package you.fileserver.dto.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ダウンロード用トークンのDTO
 */
@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DownloadTokenInfo {
    @Id
    private String token; //ダウンロードに必要な一時トークン
    private String fileID; //ダウンロードするファイルID
    private LocalDateTime expirationDate; //有効期限
}
