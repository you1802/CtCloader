package you.fileserver.service;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import you.fileserver.authentication.CustomUserDetails;
import you.fileserver.dto.FileDetail;
import you.fileserver.dto.entity.DownloadTokenInfo;
import you.fileserver.dto.entity.UploadFileInfo;
import you.fileserver.dto.entity.UploadFilePassword;
import you.fileserver.repository.DownloadTokenInfoRepository;
import you.fileserver.repository.UploadFileInfoRepository;
import you.fileserver.repository.UploadFilePasswordRepository;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

import static you.fileserver.config.Constants.*;

@Service
public class DownloadService {
    private final PasswordEncoder passwordEncoder;
    private final UploadFileInfoRepository uploadFileInfoRepository;
    private final UploadFilePasswordRepository uploadFilePasswordRepository;
    private final ThreadPoolTaskScheduler taskScheduler;
    private final DownloadTokenInfoRepository downloadTokenInfoRepository;


    public DownloadService(PasswordEncoder passwordEncoder,
                           UploadFileInfoRepository uploadFileInfoRepository,
                           UploadFilePasswordRepository uploadFilePasswordRepository,
                           ThreadPoolTaskScheduler threadPoolTaskScheduler,
                           DownloadTokenInfoRepository downloadTokenInfoRepository) {
        this.passwordEncoder = passwordEncoder;
        this.uploadFileInfoRepository = uploadFileInfoRepository;
        this.uploadFilePasswordRepository = uploadFilePasswordRepository;
        this.taskScheduler = threadPoolTaskScheduler;
        this.downloadTokenInfoRepository = downloadTokenInfoRepository;
    }

    /**
     * ダウンロード対象のファイル名とパスワードを受け取ってパスワードが一致した場合、ダウンロードに必要なトークンを返す
     * @param targetFileName ダウンロードするファイル名(実態のファイル名)
     * @param password パスワード
     * @return 結果のレスポンス
     */
    public ResponseEntity<?> passwordAuth (String targetFileName, String password) {
        //ファイルの存在チェック
        Optional<UploadFileInfo> targetFile = uploadFileInfoRepository.findById(targetFileName);
        if (targetFile.isEmpty()) return ResponseEntity.badRequest().build();
        Optional<UploadFilePassword> targetFilePassword = uploadFilePasswordRepository.findById(targetFileName);
        if (targetFilePassword.isEmpty()) return ResponseEntity.badRequest().build();

        //ダウンロードパスワード一致時の処理
        if (passwordEncoder.matches(password, targetFilePassword.get().getDownloadPassword())) {
            return ResponseEntity.ok(Map.of("code", 200, "token", createToken(targetFileName, 30))); //30秒有効期限のトークンをレスポンスとして返す
        } else {
            return ResponseEntity.ok(Map.of("code", 401, "message", "パスワードが一致しません")); //不一致の時
        }
    }

    /**
     * ファイル転送用のURLをレスポンスに入れて返す
     * @param targetFileName ダウンロードするファイル名(実態のファイル名)
     * @param userDetail ログイン中のユーザー情報
     * @return URL情報が入ったレスポンス
     */
    public ResponseEntity<?> createFileTransferUrl(String targetFileName, CustomUserDetails userDetail) {
        Optional<UploadFileInfo> targetFile = uploadFileInfoRepository.findById(targetFileName);
        if (targetFile.isEmpty() || userDetail == null || !Objects.equals(targetFile.get().getOwner(), userDetail.getUsername())) return ResponseEntity.badRequest().build(); //ファイルが自身のアップロードしたものでない場合バッドリクエスト

        return ResponseEntity.ok(Map.of("code", 200, "URL", ROOT_URL + DOWNLOAD_PATH + "?targetFileName=" + targetFileName + "&token=" + createToken(targetFileName, 86400))); //DL用URLを組み立てて返す(有効期限１日)
    }

    /**
     * ダウンロード対象ファイル名とトークンを受け取って条件を満たす場合ダウンロードさせる(ファイルがダウンロードロックされてない場合のトークンは空)
     * @param targetFileName ダウンロードするファイル名(実態のファイル名)
     * @param token トークン
     * @return ファイルデータストリーミングのレスポンス
     */
    public ResponseEntity<StreamingResponseBody> downloadFile(String targetFileName, String token) {
        Optional<UploadFileInfo> targetFileInfo = uploadFileInfoRepository.findById(targetFileName);
        Optional<DownloadTokenInfo> tokenInfo = downloadTokenInfoRepository.findById(token);

        if (targetFileInfo.isEmpty()) return ResponseEntity.status(HttpStatus.GONE).build(); //ファイルが存在しない場合は410 gone

        if (targetFileInfo.get().isDownloadLocked()) { //ファイルがダウンロードロックされている場合
            if (tokenInfo.isEmpty() || !Objects.equals(targetFileName, tokenInfo.get().getUniqueFileName()) || LocalDateTime.now().isAfter(tokenInfo.get().getExpirationDate())) {
                return ResponseEntity.status(HttpStatus.GONE).build(); //トークンが存在しないまたはトークンに紐づけされているファイル名に一致しない場合またはトークン期限切れの時は 410 gone
            }
        }

        downloadTokenInfoRepository.deleteById(token); //トークン情報を削除

        File file = Path.of(FILE_PATH, targetFileName).toFile();

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

        HttpHeaders headers = new HttpHeaders(); //ヘッダーの設定
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM); //コンテンツタイプをOCTET_STREAMに設定(このタイプが設定されているとブラウザで通常表示できる画像データなども自動でDLされる)
        headers.setContentDispositionFormData("attachment", URLEncoder.encode(targetFileInfo.get().getOriginalFileName(), StandardCharsets.UTF_8)); //ファイル名をセット
        headers.setContentLength(file.length()); //ファイルのサイズをセット(これがあると正確な残りDL時間がブラウザに表示される)

        return new ResponseEntity<>(streamingResponseBody, headers, HttpStatus.OK);
    }

    /**
     * ユーザーに提示するファイルリストの情報を返す
     * @return ファイルリスト
     */
    public List<FileDetail> fileDetails(CustomUserDetails userDetail) {
        List<UploadFileInfo> uploadFileInfoList = uploadFileInfoRepository.findAll();
        List<FileDetail> fileDetails = new ArrayList<>();

        for (UploadFileInfo fileInfo : uploadFileInfoList) {
            boolean owned;
            if (userDetail == null) {
                owned = false;
            } else owned = userDetail.getUsername().equals(fileInfo.getOwner());

            fileDetails.add(FileDetail.builder()
                    .uploadDate(fileInfo.getUploadDate())
                    .size(fileInfo.getSize())
                    .owned(owned)
                    .name(fileInfo.getOriginalFileName())
                    .uniqueFileName(fileInfo.getUniqueFileName())
                    .downloadLock(fileInfo.isDownloadLocked())
                    .build());
        }
        return fileDetails;
    }

    /**
     * ダウンロード用トークンを生成しデータベースに保存する
     * @param uniqueFileName DL対象のユニークネーム
     * @param expiration 有効期限(秒)
     * @return トークン
     */
    private String createToken(String uniqueFileName, long expiration) {
        UUID token = UUID.randomUUID(); //UUIDを取得(トークン)

        DownloadTokenInfo tokenInfo = DownloadTokenInfo.builder() //指定された有効期限のトークン情報を生成
                .token(token.toString())
                .uniqueFileName(uniqueFileName)
                .expirationDate(LocalDateTime.now().plusSeconds(expiration))
                .build();

        downloadTokenInfoRepository.save(tokenInfo);
        return tokenInfo.getToken();
    }
}
