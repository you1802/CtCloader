package you.fileserver.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import me.desair.tus.server.TusFileUploadService;
import me.desair.tus.server.exception.TusException;
import me.desair.tus.server.upload.UploadInfo;
import org.apache.commons.io.FilenameUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import you.fileserver.authentication.CustomUserDetails;
import you.fileserver.dto.entity.UploadFileInfo;
import you.fileserver.dto.entity.UploadFilePassword;
import you.fileserver.repository.UploadFileInfoRepository;
import you.fileserver.repository.UploadFilePasswordRepository;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

import static you.fileserver.config.Constants.FILE_PATH;
import static you.fileserver.config.Constants.FILE_PASSWORD_REGEX;

@Service
public class UploadService {
    TusFileUploadService tusFileUploadService;
    UploadFileInfoRepository uploadFileInfoRepository;
    UploadFilePasswordRepository uploadFilePasswordRepository;
    PasswordEncoder passwordEncoder;

    public UploadService(TusFileUploadService tusFileUploadService,
                         UploadFileInfoRepository uploadFileInfoRepository,
                         UploadFilePasswordRepository uploadFilePasswordRepository,
                         PasswordEncoder passwordEncoder) {
        this.tusFileUploadService = tusFileUploadService;
        this.uploadFileInfoRepository = uploadFileInfoRepository;
        this.uploadFilePasswordRepository = uploadFilePasswordRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 条件を満たせばアップロードしデータベースに情報を保存する
     * @param request コントローラーで受け取ったリクエスト
     * @param response レスポンス
     * @param userDetail ログイン中のユーザー情報
     * @throws IOException ファイル保存関係のエラー
     * @throws TusException TUS関係のエラー
     */
    public void upload(HttpServletRequest request, HttpServletResponse response, CustomUserDetails userDetail) throws IOException, TusException {
        tusFileUploadService.process(request, response);

        String uploadUri = request.getRequestURI();

        UploadInfo uploadInfo = tusFileUploadService.getUploadInfo(uploadUri);

        //終了時の処理
        if (uploadInfo != null && !uploadInfo.isUploadInProgress()) {
            Map<String, String> metaData = uploadInfo.getMetadata(); //ユーザーから送信されたメタデータを取得
            boolean downloadPasswordEnabled = Boolean.parseBoolean(metaData.get("downloadPasswordEnabled"));//ダウンロードパスワードが有効かどうか

            String deletePassword = metaData.get("deletePassword");
            String downloadPassword = metaData.get("downloadPassword");

            //パスワードの正規表現最終チェック(通常の使用ではこれを満たすことはない)
            if (!deletePassword.matches(FILE_PASSWORD_REGEX)) return;
            if (downloadPasswordEnabled) {
                if (!downloadPassword.matches(FILE_PASSWORD_REGEX)) return;
            }

            InputStream is = tusFileUploadService.getUploadedBytes(uploadUri);

            String originalFileName = uploadInfo.getFileName().replace(" ", "-"); //オリジナルのファイル名にあるスペースを-に変換する
            String extension = FilenameUtils.getExtension(originalFileName); //ファイルの拡張子を取得
            String uploadId = uploadUri.substring(uploadUri.lastIndexOf("/") + 1); //アップロードURIからアップロードIDを取得
            String uniqueFileName = uploadId + "." + extension; //アップロードIDに拡張子を付けて保存用のファイル名にする

            Files.copy(is, Path.of(FILE_PATH, uniqueFileName)); //保存用ファイル名でファイルを保存

            String ownerName; //ログイン中のユーザー名
            boolean visible; //可視化設定
            if (userDetail != null) {
                ownerName = userDetail.getUsername();
                visible = !Boolean.parseBoolean(metaData.get("visibleSetting"));
            } else {
                ownerName = "";
                visible = true;
            }

            //データベースにファイル情報を登録
            UploadFileInfo fileInfo = UploadFileInfo.builder()
                    .owner(ownerName)
                    .originalFileName(originalFileName)
                    .uniqueFileName(uniqueFileName)
                    .size(uploadInfo.getLength())
                    .uploadDate(LocalDateTime.now())
                    .visible(visible)
                    .downloadLocked(downloadPasswordEnabled)
                    .comment(metaData.get("comment"))
                    .ipAddress(request.getRemoteAddr())
                    .build();
            uploadFileInfoRepository.save(fileInfo);

            //データベースにパスワードを登録
            UploadFilePassword ufp = UploadFilePassword.builder()
                    .uniqueFileName(uniqueFileName)
                    .deletePassword(passwordEncoder.encode(deletePassword))
                    .downloadPassword(downloadPasswordEnabled ? passwordEncoder.encode(downloadPassword) : "")
                    .build();
            uploadFilePasswordRepository.save(ufp);
        }
        tusFileUploadService.deleteUpload(uploadUri);
    }
}
