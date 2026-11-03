package you.fileserver.dto.entity;

import jakarta.persistence.*;
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
    private Long id; //ID
    private String fileID; //ファイルID
    private String comment; //ファイルに対する問題
    private String ipAddress; //報告者のIP

    @Tolerate
    public FileReport() {}
}
