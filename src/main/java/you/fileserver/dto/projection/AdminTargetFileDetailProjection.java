package you.fileserver.dto.projection;

import you.fileserver.dto.entity.FileReport;

import java.util.List;

public interface AdminTargetFileDetailProjection {
    String getComment();
    String getIpAddress();
    int getDownloadCount();
    List<FileReport> getFileReports();
}
