package you.fileserver.dto.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Tolerate;

/**
 * ユーザーアカウントのDTO
 */
@Data
@Builder
@Entity
public class UserAccount {
    @Id
    private String username; //ユーザーネーム
    private String password; //パスワード
    private String role; //ロール

    @Tolerate //builderを使いたいのでこのアノテーションでディフォルトコンストラクタをLombokに隠ぺいする
    public UserAccount() {}
}
