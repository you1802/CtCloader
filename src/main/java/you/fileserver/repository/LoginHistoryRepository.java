package you.fileserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import you.fileserver.dto.entity.LoginHistory;

import java.util.List;

@Repository
public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {
    /**
     * ユーザーの最新10件のログイン履歴を取得する
     * @param username ユーザー名
     * @param type プロジェクションクラス
     * @return リスト
     * @param <T> ジェネリックメソッド
     */
    <T> List<T> findTop10ByUsernameOrderByLoginAtDesc(String username, Class<T> type);
}
