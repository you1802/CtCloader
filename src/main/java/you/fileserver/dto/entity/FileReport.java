package you.fileserver.dto.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Tolerate;

/**
 * ファイルの問題報告用DTO
 */
@Entity
@Data
@Builder
public class FileReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String uniqueFileName;
    private String comment;
    private String ipAddress;

    @Tolerate
    public FileReport() {}
}
