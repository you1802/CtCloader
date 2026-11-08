package you.fileserver.service;

import org.springframework.stereotype.Service;
import you.fileserver.dto.UserLoginHistory;
import you.fileserver.dto.entity.LoginHistory;
import you.fileserver.repository.LoginHistoryRepository;
import you.fileserver.security.authentication.CustomUserDetails;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
     * 最新１０件のログイン履歴をDBから取得する
     * @param userDetail ログイン中ユーザー情報
     * @return リスト
     */
    public List<UserLoginHistory> getLoginHistory(CustomUserDetails userDetail) {
        Optional<CustomUserDetails> userDetailOpt = Optional.ofNullable(userDetail);

        return userDetailOpt.map(user ->
                loginHistoryRepository.findTop10ByUsernameOrderByLoginAtDesc(user.getUsername(), UserLoginHistory.class))
                .orElse(null);
    }
}
