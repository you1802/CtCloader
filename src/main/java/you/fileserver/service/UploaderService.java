package you.fileserver.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import me.desair.tus.server.TusFileUploadService;
import me.desair.tus.server.exception.TusException;
import me.desair.tus.server.upload.UploadInfo;
import org.springframework.stereotype.Service;
import you.fileserver.entity.FileDetail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static you.fileserver.config.Constants.FILE_PATH;

@Service
public class UploaderService {
    TusFileUploadService tusFileUploadService;

    public UploaderService(TusFileUploadService tusFileUploadService) {
        this.tusFileUploadService = tusFileUploadService;
    }

    public void upload(HttpServletRequest request, HttpServletResponse response) throws IOException, TusException {
        tusFileUploadService.process(request, response);

        String uploadUri = request.getRequestURI();

        UploadInfo uploadInfo = tusFileUploadService.getUploadInfo(uploadUri);

        if (uploadInfo != null && !uploadInfo.isUploadInProgress()) {
            InputStream is = tusFileUploadService.getUploadedBytes(uploadUri);
            String fileName = uploadInfo.getFileName().replace(" ", "-");
            Files.copy(is, Path.of(FILE_PATH, fileName));
        }

        tusFileUploadService.deleteUpload(uploadUri);
    }

    //テスト用
    public List<FileDetail> fileDetails() {
        List<FileDetail> fileDetails = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            fileDetails.add(FileDetail.builder()
                    .name("ファイル" + i)
                    .size(123456 * i)
                    .owner(i % 2 == 0)
                    .lastModifiedDate(LocalDateTime.now().minusDays(i)).
                    build());
        }
        return fileDetails;
    }
}
