package you.fileserver.exception;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import you.fileserver.dto.DefaultApiResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(TargetFileNotFoundException.class)
    public DefaultApiResponse handleFileNotFoundException(TargetFileNotFoundException e) {
        return new DefaultApiResponse("404", "ダウンロードしようとしたファイルが見つかりません ID:" + e.getMessage());
    }
}
