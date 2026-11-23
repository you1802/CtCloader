package you.fileserver.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
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
import java.text.BreakIterator;
import java.time.LocalDateTime;
import java.util.*;

import static you.fileserver.config.Constants.FILE_PATH;
import static you.fileserver.config.Constants.FILE_PASSWORD_REGEX;

/**
 * アップロード関係のサービスクラス
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UploadService {
    private final TusFileUploadService tusFileUploadService;
    private final UploadFileInfoRepository uploadFileInfoRepository;
    private final UploadFilePasswordRepository uploadFilePasswordRepository;
    private final PasswordEncoder passwordEncoder;

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
            boolean downloadPasswordEnabled = false; //ダウンロードパスワード有効か
            boolean login = userDetail != null; //ログイン中かどうか
            boolean visible = !(Boolean.parseBoolean(metaData.get("visibleSetting")) && login); //公開設定、非公開設定はログイン中のみ有効
            String owner = login ? userDetail.getUsername() : ""; //アップロードしたユーザー名

            String deletePassword = metaData.get("deletePassword"); //削除用パスワード
            String downloadPassword = metaData.get("downloadPassword"); //ダウンロード用パスワード

            if (visible) { //公開設定の場合
                downloadPasswordEnabled = Boolean.parseBoolean(metaData.get("downloadPasswordEnabled")); //ダウンロードパスワードが有効かどうか
                //パスワードの正規表現最終チェック(通常の使用ではここで弾かれることはない)
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
            }

            InputStream is = tusFileUploadService.getUploadedBytes(uploadUri); //インプットストリームを取得

            String filename = sanitizeFilename(uploadInfo.getFileName()); //ファイル名をサニタイズする
            String fileID = uploadUri.substring(uploadUri.lastIndexOf("/") + 1); //アップロードURIからアップロードIDを取得しファイルのIDに

            Files.copy(is, Path.of(FILE_PATH, fileID)); //保存用ファイル名(ID)でファイルを保存

            LocalDateTime now = LocalDateTime.now(); //現在時刻を取得

            //データベースにファイル情報を登録
            UploadFileInfo fileInfo = UploadFileInfo.builder()
                    .owner(owner)
                    .filename(filename)
                    .fileID(fileID)
                    .size(uploadInfo.getLength())
                    .uploadDate(now)
                    .visible(visible)
                    .downloadLocked(downloadPasswordEnabled)
                    .comment(truncateComment(metaData.get("comment")))
                    .ipAddress(request.getRemoteAddr())
                    .downloadCount(0)
                    .lastDownloadDate(now)
                    .build();
            uploadFileInfoRepository.save(fileInfo);

            //データベースにパスワードを登録
            UploadFilePassword ufp = UploadFilePassword.builder()
                    .fileID(fileID)
                    .deletePassword(login ? "" : passwordEncoder.encode(deletePassword)) //ログイン中なら削除パスは不要
                    .downloadPassword(downloadPasswordEnabled ? passwordEncoder.encode(downloadPassword) : "") //ダウンロードロックされてない場合,または非公開の場合はパス不要
                    .build();
            uploadFilePasswordRepository.save(ufp);

            log.info("file upload complete ip:{} user:{} fileID:{}", request.getRemoteAddr(), login ? userDetail.getUsername() : null, fileID); //ログを出力
        }
        tusFileUploadService.deleteUpload(uploadUri);
    }

    /**
     * ファイル名をサニタイズする
     * @param filename ファイル名
     * @return 無害化されたファイル名
     */
    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) return "unnamed"; //NULL・空・空白のみの場合はunnamed
        String sanitized = filename.replaceAll("\\p{Cntrl}", "") // 制御文字を除去
                .replace(" ", "-"); //スペースは-に変換
        if (sanitized.isEmpty()) return "unnamed"; //制御文字のみのファイルの場合空になるのでその時はunnamed
        return sanitized.length() > 255 ? sanitized.substring(0, 255) : sanitized; //ファイル名は２５５文字まで
    }

    /**
     * コメントをサロゲートペアや書記素クラスタを考慮しつつ１００文字までに制限する(超えた場合は100文字で切って…を付けて返す)
     * @param comment コメント
     * @return 編集済みのコメント
     */
    private String truncateComment(String comment) {
        if (comment == null) return null; //nullならnullをかえす。

        BreakIterator breakIterator = BreakIterator.getCharacterInstance(Locale.JAPANESE); //文字単位の境界を判定するイテレータを取得
        breakIterator.setText(comment); //コメントをセット

        int endIndex = breakIterator.next(100); //先頭から数えて100個目の境界の位置を取得

        if (endIndex == BreakIterator.DONE || endIndex == comment.length()) return comment; //見た目上の文字数の長さが100以下ならコメントをそのままかえす

        return comment.substring(0, endIndex) + "…"; //100以上の時は100で切って…を付けて返す
    }
}