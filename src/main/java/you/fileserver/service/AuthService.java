package you.fileserver.service;

import org.springframework.stereotype.Service;
import you.fileserver.authentication.UserDetailServiceImpl;
import you.fileserver.repository.UserAccountRepository;

import java.util.Map;

import static you.fileserver.config.Constants.PASSWORD_REGEX;

@Service
public class AuthService {
    private final UserDetailServiceImpl userDetailsServiceImpl;
    private final UserAccountRepository userAccountRepository;

    public AuthService(UserDetailServiceImpl userDetailsServiceImpl, UserAccountRepository userAccountRepository) {
        this.userDetailsServiceImpl = userDetailsServiceImpl;
        this.userAccountRepository = userAccountRepository;
    }

    public Map<String, Object> register(String username, String password) {
        //正規表現の最終チェック(クライアントでもチェックする)
        if (!username.matches(PASSWORD_REGEX) || !password.matches(PASSWORD_REGEX)) return Map.of("code", "400", "message", "正規表現エラー");

        return userDetailsServiceImpl.saveUserAccount(username, password) ? Map.of("code", 200, "message", "登録完了しました") : Map.of("code", 400, "message", "既に登録されてるユーザー名です");
    }

    //ユーザーネーム使用済みチェック
    public Map<String, Object> userNameExists(String username) {
        return userAccountRepository.existsById(username) ? Map.of("exists", true) : Map.of("exists", false);
    }
}
