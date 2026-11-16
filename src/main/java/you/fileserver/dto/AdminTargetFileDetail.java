package you.fileserver.dto;

import lombok.Builder;
import lombok.Data;
import you.fileserver.dto.entity.FileReport;

import java.util.List;

@Data
@Builder
public class AdminTargetFileDetail {
    private String comment; //ファイルのコメント
    private String ipAddress; //アップロード者のIP
    private int downloadCount; //ダウンロード数
    private List<FileReport> fileReports; //ファイルの問題報告リスト
}