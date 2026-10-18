package you.fileserver.service;

import org.springframework.stereotype.Service;
import you.fileserver.authentication.CustomUserDetails;
import you.fileserver.authentication.UserDetailServiceImpl;
import you.fileserver.repository.UserAccountRepository;
import you.fileserver.utility.FileSizeLimitCalculator;

import java.util.Map;

import static you.fileserver.config.Constants.*;

@Service
public class AuthService {
    private final UserDetailServiceImpl userDetailsServiceImpl;
    private final UserAccountRepository userAccountRepository;
    private final FileSizeLimitCalculator fileSizeLimitCalculator;

    public AuthService(UserDetailServiceImpl userDetailsServiceImpl, UserAccountRepository userAccountRepository, FileSizeLimitCalculator fileSizeLimitCalculator) {
        this.userDetailsServiceImpl = userDetailsServiceImpl;
        this.userAccountRepository = userAccountRepository;
        this.fileSizeLimitCalculator = fileSizeLimitCalculator;
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
}
