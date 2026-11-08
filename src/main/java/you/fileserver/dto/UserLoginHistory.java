package you.fileserver.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@AllArgsConstructor
@Data
public class UserLoginHistory {
    private LocalDateTime loginAt; //ログイン日時
    private String ip; //接続元IP
    private boolean success; //ログイン成功か失敗か
}
