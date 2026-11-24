package you.fileserver.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import you.fileserver.dto.DefaultApiResponse;
import you.fileserver.dto.entity.FileReport;
import you.fileserver.repository.FileReportRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * ReportService の単体テスト。保存1件＋固定レスポンスだけの
 * 薄いサービスなので、保存内容の検証が中心。
 * truncateCommentはアップロード時のコメントの処理と同じ処理なのでそちらでカバー
 */
@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private FileReportRepository fileReportRepository;

    @InjectMocks
    private ReportService reportService;

    @Test
    void saveReport_savesReportAndReturns200() {
        DefaultApiResponse response = reportService.saveReport("file1", "不適切な内容です", "127.0.0.1");

        assertEquals(200, response.getCode());
        assertEquals("問題の報告に成功しました。", response.getMessage());

        ArgumentCaptor<FileReport> captor = ArgumentCaptor.forClass(FileReport.class);
        verify(fileReportRepository).save(captor.capture());
        FileReport saved = captor.getValue();
        assertEquals("file1", saved.getFileID());
        assertEquals("不適切な内容です", saved.getComment());
        assertEquals("127.0.0.1", saved.getIpAddress());
    }
}