package you.fileserver.service;

import org.springframework.stereotype.Service;
import you.fileserver.authentication.UserDetailServiceImpl;

import java.util.Map;

import static you.fileserver.config.Constants.PASSWORD_REGEX;

@Service
public class AuthService {
    private final UserDetailServiceImpl userDetailsServiceImpl;

    public AuthService(UserDetailServiceImpl userDetailsServiceImpl) {
        this.userDetailsServiceImpl = userDetailsServiceImpl;
    }

    public Map<String, Object> register(String username, String password) {
        //正規表現の最終チェック(クライアントでもチェックする)
        if (!username.matches(PASSWORD_REGEX) || !password.matches(PASSWORD_REGEX)) return Map.of("code", "400", "message", "正規表現エラー");

        return userDetailsServiceImpl.saveUserAccount(username, password) ? Map.of("code", 200, "message", "登録完了しました") : Map.of("code", 400, "message", "既に登録されてるユーザー名です");
    }
}
