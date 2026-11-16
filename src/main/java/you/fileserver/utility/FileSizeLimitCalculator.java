package you.fileserver.utility;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import you.fileserver.repository.UserAccountRepository;

import java.util.Map;

import static you.fileserver.config.Constants.*;

@Component
@RequiredArgsConstructor
public class FileSizeLimitCalculator {
    private final UserAccountRepository userAccountRepository;

    /**
     * ユーザー登録の時サーバーのサイズ上限を超えないかをチェックする
     * @return trueなら登録不可
     */
    public boolean userAccountLimit() {
        return (USER_FILESIZE_LIMIT * (userAccountRepository.findAll().size() + 1) + FORUM_FILESIZE_LIMIT) > SERVER_SIZE_LIMIT;
    }

    /**
     * ユーザー登録可能かどうかの情報をmapでかえす
     * @return enable, trueなら登録可能
     */
    public Map<String, Object> enableCreateAccount() {
        return userAccountLimit() ? Map.of("enable", false) : Map.of("enable", true);
    }
}