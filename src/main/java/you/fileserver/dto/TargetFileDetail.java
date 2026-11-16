package you.fileserver.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 個別ファイル情報用DTO
 */
@Data
@Builder
public class TargetFileDetail {
    private String comment; //ファイルのコメント
    private int downloadCount; //ダウンロード数
}
