package you.fileserver.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import me.desair.tus.server.exception.TusException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import you.fileserver.authentication.CustomUserDetails;
import you.fileserver.dto.FileDetail;
import you.fileserver.service.AuthService;
import you.fileserver.service.DownloadService;
import you.fileserver.service.UploadService;
import you.fileserver.service.DeleteService;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static you.fileserver.config.Constants.*;

@Controller
public class FileServerController {
    private final UploadService uploadService;
    private final DownloadService downloadService;
    private final DeleteService deleteService;
    private final AuthService authService;

    public FileServerController(UploadService uploadService, DownloadService DownloadService, AuthService authService, DeleteService deleteService) {
        this.uploadService = uploadService;
        this.downloadService = DownloadService;
        this.authService = authService;
        this.deleteService = deleteService;
    }

    //全ての機能があるページ
    @GetMapping(UPLOADER_ROOT_PATH)
    public String file() {
        return "ctcloader";
    }

    //ファイルリスト取得用API
    @GetMapping(FILE_DETAILS_JSON_PATH)
    @ResponseBody
    public List<FileDetail> fileDetails(@AuthenticationPrincipal CustomUserDetails userDetail) {
        return downloadService.fileDetails(userDetail);
    }

    //アップロード用エンドポイント
    @RequestMapping(value = UPLOAD_PATH, method = {
            RequestMethod.POST, RequestMethod.PATCH, RequestMethod.HEAD, RequestMethod.DELETE, RequestMethod.OPTIONS
    })
    public void upload(HttpServletRequest request, HttpServletResponse response, @AuthenticationPrincipal CustomUserDetails userDetail) throws TusException, IOException {
        uploadService.upload(request, response, userDetail);
    }

    //ダウンロード用のトークン発行するAPI
    @PostMapping(DOWNLOAD_AUTH_PATH)
    public ResponseEntity<?> downloadAuth(@RequestParam String targetFileName, @RequestParam String fileControlPassword) {
        return downloadService.passwordAuth(targetFileName, fileControlPassword);
    }

    //ファイル転送用URLを発行するAPI
    @PostMapping(TRANSFER_PATH)
    public ResponseEntity<?> transferUrl(@RequestParam String targetFileName, @AuthenticationPrincipal CustomUserDetails userDetail) {
        return downloadService.createFileTransferUrl(targetFileName, userDetail);
    }

    //ダウンロード用API
    @GetMapping(DOWNLOAD_PATH)
    public ResponseEntity<StreamingResponseBody> download(@RequestParam String targetFileName, @RequestParam String token) {
        return downloadService.downloadFile(targetFileName, token);
    }

    //削除用API
    @PostMapping(DELETE_PATH)
    @ResponseBody
    public Map<String, Object> delete(@RequestParam String targetFileName, @RequestParam String fileControlPassword) {
        return deleteService.deleteFile(targetFileName, fileControlPassword);
    }

    //ユーザー登録用API
    @PostMapping(REGISTER_PATH)
    @ResponseBody
    public Map<String, Object> register(@RequestParam String username, @RequestParam String password) {
        return authService.register(username, password);
    }

    //ユーザーネームが登録済みかをチェックするAPI
    @GetMapping(USER_NAME_EXISTS_PATH)
    @ResponseBody
    public Map<String, Object> UserNameExists(@RequestParam String username) {
        return authService.userNameExists(username);
    }

    //ログイン中のユーザー名取得用API
    @GetMapping(USER_ID_PATH)
    @ResponseBody
    public Map<String, Object> getUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return Map.of("user", "ゲスト");
        }
        return Map.of("user", userDetails.getUsername());
    }
}
