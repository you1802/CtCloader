package you.fileserver.dto.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * アップロードされたファイルのパスワード用DTO
 */
@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UploadFilePassword {
    @Id
    private String fileID; //ファイルid
    private String downloadPassword; //ダウンロードパスワード
    private String deletePassword; //削除パスワード
}
