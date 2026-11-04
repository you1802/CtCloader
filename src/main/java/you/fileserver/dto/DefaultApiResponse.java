package you.fileserver.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * APIレスポンス用DTO
 */
@Data
@AllArgsConstructor
public class DefaultApiResponse {
    private int code;
    private String message;
}
