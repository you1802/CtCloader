package you.fileserver.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import you.fileserver.dto.DefaultApiResponse;
import you.fileserver.dto.projection.DeleteFileInfoProjection;
import you.fileserver.repository.UploadFileInfoRepository;
import you.fileserver.repository.UploadFilePasswordRepository;
import you.fileserver.security.annotation.IsUser;
import you.fileserver.security.authentication.CustomUserDetails;
import you.fileserver.security.authentication.UserDetailServiceImpl;
import you.fileserver.repository.UserAccountRepository;
import you.fileserver.utility.FileSizeLimitCalculator;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static you.fileserver.config.Constants.*;

/**
 * 認可認証関係のサービスクラス
 */
@Service
@Slf4j
public class AuthService {
    private final UserDetailServiceImpl userDetailsServiceImpl;
    private final UserAccountRepository userAccountRepository;
    private final FileSizeLimitCalculator fileSizeLimitCalculator;
    private final UploadFileInfoRepository uploadFileInfoRepository;
    private final UploadFilePasswordRepository uploadFilePasswordRepository;

    public AuthService(UserDetailServiceImpl userDetailsServiceImpl,
                       UserAccountRepository userAccountRepository,
                       FileSizeLimitCalculator fileSizeLimitCalculator,
                       UploadFileInfoRepository uploadFileInfoRepository,
                       UploadFilePasswordRepository uploadFilePasswordRepository) {
        this.userDetailsServiceImpl = userDetailsServiceImpl;
        this.userAccountRepository = userAccountRepository;
        this.fileSizeLimitCalculator = fileSizeLimitCalculator;
        this.uploadFileInfoRepository = uploadFileInfoRepository;
        this.uploadFilePasswordRepository = uploadFilePasswordRepository;
    }

    /**
     * 会員登録用メソッド
     * @param username 登録するユーザー名
     * @param password 登録するパスワード
     * @return 成功または失敗のレスポンス
     */
    public Map<String, Object> register(String username, String password) {
        //正規表現の最終チェック(クライアントでもチェックする)
        if (!username.matches(USER_NAME_REGEX) || !password.matches(PASSWORD_REGEX)) return Map.of("code", "400", "message", "正規表現エラー");
        //サーバーの容量上限の最終チェック
        if (fileSizeLimitCalculator.userAccountLimit()) return Map.of("code", "400", "message", "ユーザー数上限に達しました");

        return userDetailsServiceImpl.saveUserAccount(username, password) ? Map.of("code", 200, "message", "登録完了しました") : Map.of("code", 400, "message", "既に登録されてるユーザー名です");
    }

    /**
     * ユーザーネーム使用済みチェック
     * @param username チェックするユーザー名
     * @return 結果のレスポンス
     */
    public Map<String, Object> userNameExists(String username) {
        return userAccountRepository.existsById(username) ? Map.of("exists", true) : Map.of("exists", false);
    }

    /**
     * ログイン中のユーザー名を返す(非ログインなら「ゲスト」とかえす)
     * @param userDetail ログイン中のユーザー情報
     * @return ユーザー名
     */
    public Map<String, Object> getUser(CustomUserDetails userDetail) {
        if (userDetail == null) {
            return Map.of("user", "ゲスト");
        }
        return Map.of("user", userDetail.getUsername());
    }

    /**
     * ユーザー削除用メソッド,付随してユーザーがアップロードした不可視設定のファイルも削除する
     * @param userDetail ログインユーザー
     * @return レスポンス
     */
    @IsUser
    public DefaultApiResponse deleteUser(CustomUserDetails userDetail) {
        //削除するユーザーの不可視設定ファイルをリスト化
        List<DeleteFileInfoProjection> userFiles = uploadFileInfoRepository.findByOwnerOrVisible(userDetail.getUsername(), true, DeleteFileInfoProjection.class);

        userFiles.forEach(file -> {
            String fileID = file.getFileID(); //対象ファイルのIDを取得
            uploadFileInfoRepository.deleteById(fileID); //アップロードファイル情報を削除
            uploadFilePasswordRepository.deleteById(fileID); //アップロードファイルパスワード情報を削除

            //ファイル実体を削除
            File targetFile = Path.of(FILE_PATH, fileID).toFile();
            targetFile.delete();
        });

        userAccountRepository.deleteById(userDetail.getUsername()); //ユーザー情報を削除

        log.info("delete user {} success", userDetail.getUsername()); //ログを出力

        return new DefaultApiResponse("200", "ユーザーの削除に成功しました。");
    }
}
