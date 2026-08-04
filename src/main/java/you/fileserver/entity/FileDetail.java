package you.fileserver.entity;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Builder
@Data
public class FileDetail {
    private String name;
    private LocalDateTime lastModifiedDate;
    private long size;
    private boolean owner;
}
