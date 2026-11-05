package you.fileserver.service;

import org.springframework.stereotype.Service;
import you.fileserver.dto.DefaultApiResponse;
import you.fileserver.dto.entity.FileReport;
import you.fileserver.repository.FileReportRepository;

/**
 * ファイルの問題報告関係のサービスクラス
 */
@Service
public class ReportService {
    private final FileReportRepository fileReportRepository;

    public ReportService(FileReportRepository fileReportRepository) {
        this.fileReportRepository = fileReportRepository;
    }

    /**
     * ユーザーから受け取った報告をDBに登録する
     * @param fileID 対象ファイル名
     * @param comment コメント
     * @param ipAddress IP
     */
    public DefaultApiResponse saveReport(String fileID, String comment, String ipAddress) {
        fileReportRepository.save(FileReport.builder()
                .fileID(fileID)
                .comment(comment)
                .ipAddress(ipAddress)
                .build());
        return new DefaultApiResponse(200, "問題の報告に成功しました。");
    }
}
