package you.fileserver.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AllFileDetails {
    private long forumFileSize;
    private long userFileSize;
}
