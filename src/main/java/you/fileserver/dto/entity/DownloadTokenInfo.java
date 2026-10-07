package you.fileserver.dto.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Tolerate;

import java.time.LocalDateTime;

@Data
@Entity
@Builder
public class DownloadTokenInfo {
    @Id
    private String token;
    private String uniqueFileName;
    private LocalDateTime expirationDate;

    @Tolerate
    public DownloadTokenInfo() {}
}
