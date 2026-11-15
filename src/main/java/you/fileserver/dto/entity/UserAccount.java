package you.fileserver.dto.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ユーザーアカウントのDTO
 */
@Data
@Builder
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class UserAccount {
    @Id
    private String username; //ユーザーネーム
    private String password; //パスワード
    private String role; //ロール
}
