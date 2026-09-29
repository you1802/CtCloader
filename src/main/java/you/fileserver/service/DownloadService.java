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
import you.fileserver.dto.entity.UploadFileInfo;
import you.fileserver.dto.entity.UploadFilePassword;
import you.fileserver.repository.UploadFileInfoRepository;
import you.fileserver.repository.UploadFilePasswordRepository;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static you.fileserver.config.Constants.FILE_PATH;

@Service
public class DownloadService {
    private final PasswordEncoder passwordEncoder;
    private final UploadFileInfoRepository uploadFileInfoRepository;
    private final UploadFilePasswordRepository uploadFilePasswordRepository;
    private final ThreadPoolTaskScheduler taskScheduler;

    private final Map<String, String> tokenInfoMap = new ConcurrentHashMap<>(); //トークン管理用のスレッドセーフなMap(key=token, value=uniqueFileName)

    public DownloadService(PasswordEncoder passwordEncoder, UploadFileInfoRepository uploadFileInfoRepository, UploadFilePasswordRepository uploadFilePasswordRepository, ThreadPoolTaskScheduler threadPoolTaskScheduler) {
        this.passwordEncoder = passwordEncoder;
        this.uploadFileInfoRepository = uploadFileInfoRepository;
        this.uploadFilePasswordRepository = uploadFilePasswordRepository;
        this.taskScheduler = threadPoolTaskScheduler;
    }

    /**
     * ダウンロード対象のファイル名とパスワードを受け取ってパスワードが一致した場合、ダウンロードに必要なトークンを返す
     * @param targetFileName ダウンロードするファイル名(実態のファイル名)
     * @param password パスワード
     * @return 結果のレスポンス
     */
    public ResponseEntity<?> passwordAuth (String targetFileName, String password){
        //ファイルの存在チェック
        Optional<UploadFileInfo> targetFile = uploadFileInfoRepository.findById(targetFileName);
        if (targetFile.isEmpty()) return ResponseEntity.badRequest().build();
        Optional<UploadFilePassword> targetFilePassword = uploadFilePasswordRepository.findById(targetFileName);
        if (targetFilePassword.isEmpty()) return ResponseEntity.badRequest().build();

        //ダウンロードパスワード一致時の処理
        if (passwordEncoder.matches(password, targetFilePassword.get().getDownloadPassword())) {
            String token = UUID.randomUUID().toString(); //UUIDを取得
            tokenInfoMap.put(token, targetFile.get().getUniqueFileName()); //トークン管理用MAPに登録

            //３０秒後にトークンを自動削除
            Instant deleteTime = Instant.now().plusSeconds(30);
            taskScheduler.schedule(() -> {
                tokenInfoMap.remove(token);
            }, deleteTime);

            return ResponseEntity.ok(Map.of("code", 200, "token", token)); //トークンをレスポンスとして返す
        } else {
            return ResponseEntity.ok(Map.of("code", 401, "message", "パスワードが一致しません")); //不一致の時
        }
    }

    /**
     * ダウンロード対象ファイル名とトークンを受け取って条件を満たす場合ダウンロードさせる(ファイルがダウンロードロックされてない場合のトークンは空)
     * @param targetFileName ダウンロードするファイル名(実態のファイル名)
     * @param token 発行したトークン
     * @return ファイルデータストリーミングのレスポンス
     */
    public ResponseEntity<StreamingResponseBody> downloadFile (String targetFileName, String token){
        Optional<UploadFileInfo> targetFileInfo = uploadFileInfoRepository.findById(targetFileName);

        if (targetFileInfo.isEmpty()) return ResponseEntity.status(HttpStatus.GONE).build(); //ファイルが存在しない場合は410 gone

        String targetUniqueFilename = tokenInfoMap.get(token);

        if (targetFileInfo.get().isDownloadLocked()) { //ファイルがダウンロードロックされている場合
            if (targetUniqueFilename == null || !Objects.equals(targetFileInfo.get().getUniqueFileName(), targetUniqueFilename)) {
                return ResponseEntity.status(HttpStatus.GONE).build(); //トークン情報が存在しないか,トークンに紐づけされているファイル名に一致しない場合 410 gone
            }
        }

        tokenInfoMap.remove(token); //トークン情報を削除

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
}
