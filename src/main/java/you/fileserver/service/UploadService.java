package you.fileserver.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import me.desair.tus.server.TusFileUploadService;
import me.desair.tus.server.exception.TusException;
import me.desair.tus.server.upload.UploadInfo;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import you.fileserver.security.authentication.CustomUserDetails;
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

/**
 * アップロード関係のサービスクラス
 */
@Service
@Slf4j
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
            boolean login = userDetail != null; //ログイン中かどうか

            String deletePassword = metaData.get("deletePassword");
            String downloadPassword = metaData.get("downloadPassword");

            //パスワードの正規表現最終チェック(通常の使用ではこれを満たすことはない)
            if (!login) { //ログイン中ではない場合
                if (!deletePassword.matches(FILE_PASSWORD_REGEX)) { //正規表現チェック
                    log.warn("file delete password regex not match password:{}", deletePassword); //ログを出力
                    return;
                }
            }

            if (downloadPasswordEnabled) { //ダウンロードパスワードロックが有効の場合
                if (!downloadPassword.matches(FILE_PASSWORD_REGEX)) { //正規表現チェック
                    log.warn("file download password regex not match password:{}", downloadPassword); //ログを出力
                    return;
                }
            }

            InputStream is = tusFileUploadService.getUploadedBytes(uploadUri); //インプットストリームを取得

            String filename = uploadInfo.getFileName().replace(" ", "-"); //オリジナルのファイル名にあるスペースを-に変換する
            String fileID = uploadUri.substring(uploadUri.lastIndexOf("/") + 1); //アップロードURIからアップロードIDを取得しファイルのIDに

            Files.copy(is, Path.of(FILE_PATH, fileID)); //保存用ファイル名(ID)でファイルを保存

            String ownerName; //ログイン中のユーザー名
            boolean visible; //可視化設定
            if (login) {
                ownerName = userDetail.getUsername();
                visible = !Boolean.parseBoolean(metaData.get("visibleSetting"));
            } else {
                ownerName = "";
                visible = true;
            }

            LocalDateTime now = LocalDateTime.now();

            //データベースにファイル情報を登録
            UploadFileInfo fileInfo = UploadFileInfo.builder()
                    .owner(ownerName)
                    .filename(filename)
                    .fileID(fileID)
                    .size(uploadInfo.getLength())
                    .uploadDate(now)
                    .visible(visible)
                    .downloadLocked(downloadPasswordEnabled)
                    .comment(metaData.get("comment"))
                    .ipAddress(request.getRemoteAddr())
                    .downloadCount(0)
                    .lastDownloadDate(now)
                    .build();
            uploadFileInfoRepository.save(fileInfo);

            //データベースにパスワードを登録
            UploadFilePassword ufp = UploadFilePassword.builder()
                    .fileID(fileID)
                    .deletePassword(login ? "" : passwordEncoder.encode(deletePassword)) //ログイン中なら削除パスは不要
                    .downloadPassword(downloadPasswordEnabled ? passwordEncoder.encode(downloadPassword) : "") //ダウンロードロックされてない場合はパス不要
                    .build();
            uploadFilePasswordRepository.save(ufp);

            log.info("file upload complete ip:{} user:{} fileID:{}", request.getRemoteAddr(), login ? userDetail.getUsername() : null, fileID); //ログを出力
        }
        tusFileUploadService.deleteUpload(uploadUri);
    }
}
