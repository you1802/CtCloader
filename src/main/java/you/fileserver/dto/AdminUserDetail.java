package you.fileserver.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class AdminUserDetail {
    String username; //ユーザー名
    LocalDateTime LastLoginAt; //最終ログイン日時
}