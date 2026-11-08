package you.fileserver.security;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;
import you.fileserver.service.LoginHistoryService;

/**
 * ログイン成功または失敗時のイベントリスナー用クラス
 */
@Component
public class LoginEventListener {
    private final LoginHistoryService loginHistoryService;

    public LoginEventListener(LoginHistoryService loginHistoryService) {
        this.loginHistoryService = loginHistoryService;
    }

    /**
     * ログイン成功時に呼び出されるメソッド
     * @param event イベント呼びたし元の情報が入る
     */
    @EventListener
    public void handleLoginSuccess(AuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName(); //ユーザー名を取得

        //ログイン元のIPを取得
        String ip = "unknown";
        if (event.getAuthentication().getDetails()instanceof WebAuthenticationDetails details) { //パターンマッチングでキャスト
            ip = details.getRemoteAddress(); //IPを取得
        }

        loginHistoryService.recordLogin(username, true, ip); //DBに情報を保存
    }

    /**
     * ログイン失敗時に呼び出されるメソッド
     * @param event イベント呼び出し元の情報が入る
     */
    @EventListener
    public void handleLoginFailure(AbstractAuthenticationFailureEvent event) {
        String username = event.getAuthentication().getName();

        String ip = "unknown";
        if (event.getAuthentication().getDetails()instanceof WebAuthenticationDetails details) {
            ip = details.getRemoteAddress();
        }

        loginHistoryService.recordLogin(username, false, ip);
    }
}
