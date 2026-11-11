package you.fileserver.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@AllArgsConstructor
@Data
public class UserLoginHistory {
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") //ブラウザにJSONを送るときに見やすい表示に変更する
    private LocalDateTime loginAt; //ログイン日時
    private String ip; //接続元IP
    private boolean success; //ログイン成功か失敗か
}
