package you.fileserver.exception;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import you.fileserver.dto.DefaultApiResponse;

/**
 * サーバー全体のエラーハンドラ
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(TargetFileNotFoundException.class)
    public DefaultApiResponse handleFileNotFoundException(TargetFileNotFoundException e) {
        return new DefaultApiResponse(404, "ダウンロードしようとしたファイルが見つかりません ID:" + e.getMessage());
    }

    @ExceptionHandler(ServerErrorException.class)
    public DefaultApiResponse handleServerErrorException(ServerErrorException e) {
        return new DefaultApiResponse(500, "サーバー内部エラー 原因:" + e.getMessage());
    }

    @ExceptionHandler(DeleteFailureException.class)
    public DefaultApiResponse handleDeleteFailureException(DeleteFailureException e) {
        return new DefaultApiResponse(500, "削除に失敗しました。" + e.getMessage());
    }
}
