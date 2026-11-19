package you.fileserver.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import you.fileserver.dto.DownloadBridgePageInfo;
import you.fileserver.dto.projection.DownloadBridgePageInfoProjection;
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
@RequiredArgsConstructor
@Slf4j
public class DownloadService {
    private final PasswordEncoder passwordEncoder;
    private final UploadFileInfoRepository uploadFileInfoRepository;
    private final UploadFilePasswordRepository uploadFilePasswordRepository;
    private final DownloadTokenInfoRepository downloadTokenInfoRepository;

    /**
     * ダウンロード対象のファイル名とパスワードを受け取ってパスワードが一致した場合または権限が適性の場合、ダウンロードに必要なトークンを返す
     * @param fileID ダウンロードするファイル名(実態のファイル名)
     * @param password パスワード
     * @return 結果のレスポンス
     */
    public ResponseEntity<?> passwordAuth(String fileID, String password, CustomUserDetails userDetail, String ipAddress) {
        //ファイルの存在チェック
        DownloadFileInfoProjection downloadFileInfo = uploadFileInfoRepository.findByFileID(fileID, DownloadFileInfoProjection.class)
                .orElseThrow(() -> new TargetFileNotFoundException(fileID));
        UploadFilePassword targetFilePassword = uploadFilePasswordRepository.findById(fileID)
                .orElseThrow(() -> new TargetFileNotFoundException(fileID));

        if (!(userDetail != null && userDetail.getUsername().equals(downloadFileInfo.getOwner()))) { //ユーザー自身でアップロードしたファイルの場合はパスワードチェックを迂回する
            if (!downloadFileInfo.isVisible()) return ResponseEntity.ok(Map.of( //自身のファイルでない場合は不可視設定のファイルにはアクセス不可
                    "code", 401,
                    "message", "このファイルへのアクセス権がありません"
            ));

            if (downloadFileInfo.isDownloadLocked()) { //ダウンロードロックされてる場合
                if (!(passwordEncoder.matches(password, targetFilePassword.getDownloadPassword()))) { //ダウンロードパスワードが一致しない場合
                    log.info("download password not match. fileID:{} ip:{}", fileID, ipAddress); //ログを出力
                    return ResponseEntity.ok(Map.of(
                            "code", 401,
                            "message", "パスワードが一致しません"));
                }
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
        if (targetFile.isEmpty() || userDetail == null || !Objects.equals(targetFile.get().getOwner(), userDetail.getUsername())) return ResponseEntity.ok(Map.of("code", 401, "message", "対象ファイルが存在しない、またはあなたがアップロードしたファイルではありません。"));

        return ResponseEntity.ok(Map.of(
                "code", 200,
                "URL", ROOT_URL + DOWNLOAD_BRIDGE_PAGE_PATH + "?fileID=" + fileID + "&token=" + createToken(fileID, 86400))); //DL用URLを組み立てて返す(有効期限１日)
    }

    /**
     * トークンとファイルidを検証して問題ない場合は、ダウンロード用クッションページに必要なデータを取得する
     * @param fileID ファイルid
     * @param token トークン
     * @return DTO
     */
    public DownloadBridgePageInfo getTransferPageInfo(String fileID, String token) {
        DownloadBridgePageInfoProjection projection = uploadFileInfoRepository.findByFileID(fileID, DownloadBridgePageInfoProjection.class)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.GONE)); //ファイル情報が存在しない場合は410
        DownloadTokenInfo tokenInfo = downloadTokenInfoRepository.findById(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.GONE)); //トークンが存在しない場合は410

        if (!tokenInfo.getFileID().equals(fileID) || LocalDateTime.now().isAfter(tokenInfo.getExpirationDate()))
            throw new ResponseStatusException(HttpStatus.GONE); //トークンとファイルID紐づいていない場合,またはトークン期限切れの場合は410

        //必要な情報をビルドしてかえす
        return DownloadBridgePageInfo.builder()
                .filename(projection.getFilename())
                .size(projection.getSize())
                .comment(projection.getComment())
                .owner(projection.getOwner())
                .expirationDate(tokenInfo.getExpirationDate())
                .DownloadUrl(ROOT_URL + DOWNLOAD_PATH + "?fileID=" + fileID + "&token=" + token)
                .build();
    }

    /**
     * ダウンロード対象ファイル名とトークンを受け取って条件を満たす場合ダウンロードさせる
     * @param fileID ダウンロードするファイル名(実態のファイル名)
     * @param token トークン
     * @return ファイルデータストリーミングのレスポンス
     */
    public ResponseEntity<StreamingResponseBody> downloadFile(String fileID, String token, String ipAddress) {
        DownloadBridgePageInfoProjection projection = uploadFileInfoRepository.findByFileID(fileID, DownloadBridgePageInfoProjection.class)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.GONE)); //ファイル情報が存在しない場合は410
        DownloadTokenInfo tokenInfo = downloadTokenInfoRepository.findById(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.GONE)); //トークンが存在しない場合は410

        if (!tokenInfo.getFileID().equals(fileID) || LocalDateTime.now().isAfter(tokenInfo.getExpirationDate()))
            throw new ResponseStatusException(HttpStatus.GONE); //トークンとファイルID紐づいていない場合,またはトークン期限切れの場合は410

        downloadTokenInfoRepository.deleteById(token); //トークン情報を削除

        uploadFileInfoRepository.updateDownloadCountAndDate(fileID, LocalDateTime.now()); //最終ダウンロード時間とダウンロード数を更新

        log.info("file downloaded. fileID:{} ip:{}", fileID, ipAddress); //ログを出力

        return createStreamingResponseEntity(fileID, projection.getFilename());
    }

    /**
     * ADMIN専用ダウンロードメソッド
     * @param fileID ファイル id
     * @return レスポンス
     */
    @IsAdmin
    public ResponseEntity<StreamingResponseBody> adminFileDownload(String fileID) {
        DownloadFileInfoProjection downloadFileInfo = uploadFileInfoRepository.findByFileID(fileID, DownloadFileInfoProjection.class) //ファイル情報が存在しない場合404
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        return createStreamingResponseEntity(fileID, downloadFileInfo.getFilename());
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

        downloadTokenInfoRepository.save(tokenInfo); //DBにトークン情報を保存
        return tokenInfo.getToken();
    }

    /**
     * 大容量ファイルをストリーミング配信するためのresponse bodyをが入ったresponse entityを作成する
     * @param fileID サーバー上のファイル名
     * @param filename　もともとのファイル名
     * @return response entity
     */
    private ResponseEntity<StreamingResponseBody> createStreamingResponseEntity(String fileID, String filename) {
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
        headers.setContentDispositionFormData("attachment", URLEncoder.encode(filename, StandardCharsets.UTF_8)); //ファイル名をセット
        headers.setContentLength(file.length()); //ファイルのサイズをセット(これがあると正確な残りDL時間がブラウザに表示される)

        return new ResponseEntity<>(streamingResponseBody, headers, HttpStatus.OK);
    }
}