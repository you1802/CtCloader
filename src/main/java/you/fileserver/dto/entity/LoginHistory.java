package you.fileserver.dto.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Tolerate;

import java.time.LocalDateTime;

/**
 * ユーザーごとのログイン履歴保存用DTO
 */
@Entity
@Data
@Builder
public class LoginHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id; //ID
    private String username; //ユーザーネーム
    private LocalDateTime loginAt; //ログイン日時
    private String ip; //接続元IP
    private boolean success; //ログイン成功か失敗か

    @Tolerate
    public LoginHistory() {}
}
