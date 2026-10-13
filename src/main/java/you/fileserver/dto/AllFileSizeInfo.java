package you.fileserver.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AllFileSizeInfo {
    private long forumFileSize;
    private long forumFileSizeLimit;
    private long userFileSize;
    private long userFileSizeLimit;
}
