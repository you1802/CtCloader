package you.fileserver.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import me.desair.tus.server.TusFileUploadService;
import me.desair.tus.server.exception.TusException;
import me.desair.tus.server.upload.UploadInfo;
import org.apache.commons.io.FilenameUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import you.fileserver.entity.FileDetail;
import you.fileserver.entity.UploadFileInfo;
import you.fileserver.repository.UploadFileInfoRepository;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static you.fileserver.config.Constants.FILE_PATH;

@Service
public class UploaderService {
    TusFileUploadService tusFileUploadService;
    UploadFileInfoRepository uploadFileInfoRepository;

    public UploaderService(TusFileUploadService tusFileUploadService, UploadFileInfoRepository uploadFileInfoRepository) {
        this.tusFileUploadService = tusFileUploadService;
        this.uploadFileInfoRepository = uploadFileInfoRepository;
    }

    public void upload(HttpServletRequest request, HttpServletResponse response) throws IOException, TusException {
        tusFileUploadService.process(request, response);

        String uploadUri = request.getRequestURI();

        UploadInfo uploadInfo = tusFileUploadService.getUploadInfo(uploadUri);

        //終了時の処理
        if (uploadInfo != null && !uploadInfo.isUploadInProgress()) {
            InputStream is = tusFileUploadService.getUploadedBytes(uploadUri);

            String originalFileName = uploadInfo.getFileName().replace(" ", "-"); //オリジナルのファイル名にあるスペースを-に変換する
            String extension = FilenameUtils.getExtension(originalFileName); //ファイルの拡張子を取得
            String uploadId = uploadUri.substring(uploadUri.lastIndexOf("/") + 1); //アップロードURIからアップロードIDを取得
            String uniqueFileName = uploadId + "." + extension; //アップロードIDに拡張子を付けて保存用のファイル名にする

            Files.copy(is, Path.of(FILE_PATH, uniqueFileName)); //保存用ファイル名でファイルを保存

            UploadFileInfo fileInfo = UploadFileInfo.builder()
                    .owner("test")
                    .originalFileName(originalFileName)
                    .uniqueFileName(uniqueFileName)
                    .size(uploadInfo.getLength())
                    .uploadDate(LocalDateTime.now())
                    .visible(true)
                    .build();
            uploadFileInfoRepository.save(fileInfo);
        }
        tusFileUploadService.deleteUpload(uploadUri);
    }


    public List<FileDetail> fileDetails() {
        List<UploadFileInfo> uploadFileInfoList = uploadFileInfoRepository.findAll();
        List<FileDetail> fileDetails = new ArrayList<>();

        for (UploadFileInfo fileInfo : uploadFileInfoList) {
            fileDetails.add(FileDetail.builder()
                    .uploadDate(fileInfo.getUploadDate())
                    .size(fileInfo.getSize())
                    .owned(true)
                    .name(fileInfo.getOriginalFileName())
                    .uniqueFileName(fileInfo.getUniqueFileName())
                    .build());
        }
        return fileDetails;
    }

    public ResponseEntity<StreamingResponseBody> download(String uniqueFileName) {
        Optional<UploadFileInfo> uploadFileInfo = uploadFileInfoRepository.findById(uniqueFileName);

        if (uploadFileInfo.isEmpty()) throw new RuntimeException();  //仮置き

        File file = Path.of(FILE_PATH, uniqueFileName).toFile();

        StreamingResponseBody streamingResponseBody = outputStream -> {
            try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", URLEncoder.encode(uploadFileInfo.get().getOriginalFileName(), StandardCharsets.UTF_8));
        headers.setContentLength(file.length());

        return new ResponseEntity<>(streamingResponseBody, headers, HttpStatus.OK);
    }

    public boolean deleteFile(String uniqueFileName) {
        Optional<UploadFileInfo> uploadFileInfo = uploadFileInfoRepository.findById(uniqueFileName);
        if (uploadFileInfo.isEmpty()) return false;

        uploadFileInfoRepository.delete(uploadFileInfo.get());
        File file = Path.of(FILE_PATH, uniqueFileName).toFile();
        return file.delete();
    }
}
