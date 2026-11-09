package you.fileserver.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 個別ファイル情報用DTO
 */
@Data
@Builder
public class TargetFileDetail {
    String comment; //ファイルのコメント
    int downloadCount; //ダウンロード数
}
