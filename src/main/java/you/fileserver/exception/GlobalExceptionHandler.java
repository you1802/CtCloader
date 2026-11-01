package you.fileserver.exception;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import you.fileserver.dto.ApiResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(TargetFileNotFoundException.class)
    public ApiResponse handleFileNotFoundException(TargetFileNotFoundException e) {
        return new ApiResponse("404", "ダウンロードしようとしたファイルが見つかりません ID:" + e.getMessage());
    }
}
