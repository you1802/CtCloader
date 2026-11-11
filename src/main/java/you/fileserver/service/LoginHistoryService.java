package you.fileserver.service;

import org.springframework.stereotype.Service;
import you.fileserver.dto.UserLoginHistory;
import you.fileserver.dto.entity.LoginHistory;
import you.fileserver.repository.LoginHistoryRepository;
import you.fileserver.security.annotation.IsUser;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ログイン履歴を保存するクラス
 */
@Service
public class LoginHistoryService {
    private final LoginHistoryRepository loginHistoryRepository;

    public LoginHistoryService(LoginHistoryRepository loginHistoryRepository) {
        this.loginHistoryRepository = loginHistoryRepository;
    }

    /**
     * ログイン情報をDBに記録する
     * @param username ユーザー名
     * @param success 成功か失敗か
     * @param IpAddress IP
     */
    public void recordLogin(String username, boolean success, String IpAddress) {
        loginHistoryRepository.save(LoginHistory.builder()
                .username(username)
                .success(success)
                .ip(IpAddress)
                .loginAt(LocalDateTime.now())
                .build());
    }

    /**
     * 最新１０件のログイン履歴をDBから取得する(ログイン時専用)
     * @param username ログイン中ユーザー情報
     * @return リスト
     */
    @IsUser
    public List<UserLoginHistory> getLoginHistory(String username) {
        return loginHistoryRepository.findTop10ByUsernameOrderByLoginAtDesc(username, UserLoginHistory.class);
    }
}
