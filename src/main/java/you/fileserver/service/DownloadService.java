package you.fileserver.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import you.fileserver.security.annotation.IsAdmin;
import you.fileserver.security.annotation.IsUser;
import you.fileserver.security.authentication.CustomUserDetails;
import you.fileserver.dto.entity.DownloadTokenInfo;
import you.fileserver.dto.entity.UploadFileInfo;
import you.fileserver.dto.entity.UploadFilePassword;
import you.fileserver.dto.projection.DownloadFileInfoProjection;
import you.fileserver.exception.TargetFileNotFoundException;
import you.fileserver.repository.DownloadTokenInfoRepository;
import you.fileserver.repository.UploadFileInfoRepository;
import you.fileserver.repository.UploadFilePasswordRepository;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

import static you.fileserver.config.Constants.*;

/**
 * ダウンロード関係のサービスクラス
 */
@Service
@Slf4j
public class DownloadService {
    private final PasswordEncoder passwordEncoder;
    private final UploadFileInfoRepository uploadFileInfoRepository;
    private final UploadFilePasswordRepository uploadFilePasswordRepository;
    private final DownloadTokenInfoRepository downloadTokenInfoRepository;

    public DownloadService(PasswordEncoder passwordEncoder,
                           UploadFileInfoRepository uploadFileInfoRepository,
                           UploadFilePasswordRepository uploadFilePasswordRepository,
                           DownloadTokenInfoRepository downloadTokenInfoRepository) {
        this.passwordEncoder = passwordEncoder;
        this.uploadFileInfoRepository = uploadFileInfoRepository;
        this.uploadFilePasswordRepository = uploadFilePasswordRepository;
        this.downloadTokenInfoRepository = downloadTokenInfoRepository;
    }

    /**
     * ダウンロード対象のファイル名とパスワードを受け取ってパスワードが一致した場合または権限が適性の場合、ダウンロードに必要なトークンを返す
     * @param fileID ダウンロードするファイル名(実態のファイル名)
     * @param password パスワード
     * @return 結果のレスポンス
     */
    public ResponseEntity<?> passwordAuth (String fileID, String password, CustomUserDetails userDetail, String ipAddress) {
        String currentUserName = userDetail == null ? null : userDetail.getUsername(); //ユーザー名を取得

        //ファイルの存在チェック
        DownloadFileInfoProjection downloadFileInfo = uploadFileInfoRepository.findByFileID(fileID, DownloadFileInfoProjection.class)
                .orElseThrow(() -> new TargetFileNotFoundException(fileID));
        UploadFilePassword targetFilePassword = uploadFilePasswordRepository.findById(fileID)
                .orElseThrow(() -> new TargetFileNotFoundException(fileID));

        if (downloadFileInfo.isDownloadLocked()) { //ダウンロードロックされてる場合
            if (!(passwordEncoder.matches(password, targetFilePassword.getDownloadPassword()) ||  Objects.equals(currentUserName, downloadFileInfo.getOwner()))) { //ダウンロードパスワードまたはアップロードユーザー名が一致しない場合
                log.info("download password not match. fileID:{} ip:{}" ,fileID , ipAddress); //ログを出力
                return ResponseEntity.ok(Map.of(
                        "code", 401, "message",
                        "パスワードが一致しません"));
            }
        }
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "token", createToken(fileID, 30))); //30秒有効期限のトークンをレスポンスとして返す
    }

    /**
     * ファイル転送用のURLをレスポンスに入れて返す
     * @param fileID ダウンロードするファイル名(実態のファイル名)
     * @param userDetail ログイン中のユーザー情報
     * @return URL情報が入ったレスポンス
     */
    @IsUser
    public ResponseEntity<?> createFileTransferUrl(String fileID, CustomUserDetails userDetail) {
        Optional<UploadFileInfo> targetFile = uploadFileInfoRepository.findById(fileID);
        if (targetFile.isEmpty() || userDetail == null || !Objects.equals(targetFile.get().getOwner(), userDetail.getUsername())) return ResponseEntity.badRequest().build(); //ファイルが自身のアップロードしたものでない場合バッドリクエスト

        return ResponseEntity.ok(Map.of(
                "code", 200,
                "URL", ROOT_URL + DOWNLOAD_PATH + "?fileID=" + fileID + "&token=" + createToken(fileID, 86400))); //DL用URLを組み立てて返す(有効期限１日)
    }

    /**
     * ダウンロード対象ファイル名とトークンを受け取って条件を満たす場合ダウンロードさせる(ファイルがダウンロードロックされてない場合のトークンは空)
     * @param fileID ダウンロードするファイル名(実態のファイル名)
     * @param token トークン
     * @return ファイルデータストリーミングのレスポンス
     */
    public ResponseEntity<StreamingResponseBody> downloadFile(String fileID, String token, String ipAddress) {
        Optional<UploadFileInfo> targetFileInfo = uploadFileInfoRepository.findById(fileID);
        Optional<DownloadTokenInfo> tokenInfo = downloadTokenInfoRepository.findById(token);

        if (targetFileInfo.isEmpty()) return ResponseEntity.status(HttpStatus.GONE).build(); //ファイルが存在しない場合は410 gone

        if (targetFileInfo.get().isDownloadLocked()) { //ファイルがダウンロードロックされている場合
            if (tokenInfo.isEmpty() || !Objects.equals(fileID, tokenInfo.get().getFileID()) || LocalDateTime.now().isAfter(tokenInfo.get().getExpirationDate())) {
                log.warn("token not match or expired. fileID:{} ip:{}" ,fileID , ipAddress);
                return ResponseEntity.status(HttpStatus.GONE).build(); //トークンが存在しないまたはトークンに紐づけされているファイル名に一致しない場合またはトークン期限切れの時は 410 gone
            }
        }

        downloadTokenInfoRepository.deleteById(token); //トークン情報を削除

        uploadFileInfoRepository.updateDownloadCountAndDate(fileID, LocalDateTime.now()); //最終ダウンロード時間とダウンロード数を更新

        log.info("file downloaded. fileID:{} ip:{}", fileID, ipAddress); //ログを出力

        return createStreamingResponseEntity(fileID, targetFileInfo.get().getOriginalFileName());
    }

    /**
     * ADMIN専用ダウンロードメソッド
     * @param fileID ファイル id
     * @return レスポンス
     */
    @IsAdmin
    public ResponseEntity<StreamingResponseBody> adminFileDownload(String fileID) {
        DownloadFileInfoProjection downloadFileInfo = uploadFileInfoRepository.findByFileID(fileID, DownloadFileInfoProjection.class)
                .orElseThrow(() -> new TargetFileNotFoundException(fileID));

        return createStreamingResponseEntity(fileID, downloadFileInfo.getOriginalFileName());
    }

    /**
     * ダウンロード用トークンを生成しデータベースに保存する
     * @param fileID DL対象のユニークネーム
     * @param expiration 有効期限(秒)
     * @return トークン
     */
    private String createToken(String fileID, long expiration) {
        UUID token = UUID.randomUUID(); //UUIDを取得(トークン)

        DownloadTokenInfo tokenInfo = DownloadTokenInfo.builder() //指定された有効期限のトークン情報を生成
                .token(token.toString())
                .fileID(fileID)
                .expirationDate(LocalDateTime.now().plusSeconds(expiration))
                .build();

        downloadTokenInfoRepository.save(tokenInfo);
        return tokenInfo.getToken();
    }

    /**
     * 大容量ファイルをストリーミング配信するためのresponse bodyをが入ったresponse entityを作成する
     * @param fileID サーバー上のファイル名
     * @param originalFileName　もともとのファイル名
     * @return response entity
     */
    private ResponseEntity<StreamingResponseBody> createStreamingResponseEntity(String fileID, String originalFileName) {
        File file = Path.of(FILE_PATH, fileID).toFile(); //サーバー内のディレクトリと対象ファイル名からファイルオブジェクトを作成

        StreamingResponseBody streamingResponseBody = outputStream -> {
            try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
                byte[] buffer = new byte[8192]; //バッファ8kB
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) { //対象ファイルからバッファ分読み込んでクライアントへの出力ストリームに書き込むのをファイルのデータが終わるまで繰り返す
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush(); //残ったデータの書き込み
            }
        };

        HttpHeaders headers = new HttpHeaders(); //ヘッダーの設定
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM); //コンテンツタイプをOCTET_STREAMに設定(このタイプが設定されているとブラウザで通常表示できる画像データなども自動でDLされる)
        headers.setContentDispositionFormData("attachment", URLEncoder.encode(originalFileName, StandardCharsets.UTF_8)); //ファイル名をセット
        headers.setContentLength(file.length()); //ファイルのサイズをセット(これがあると正確な残りDL時間がブラウザに表示される)

        return new ResponseEntity<>(streamingResponseBody, headers, HttpStatus.OK);
    }

    /**
     * 期限切れトークンの削除(１時間に１回実行)
     */
    @Scheduled(fixedRate = 3600000)
    private void deleteExpiredTokens() {
        downloadTokenInfoRepository.deleteByExpirationDateLessThan(LocalDateTime.now());
    }
}
