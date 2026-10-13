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
import you.fileserver.dto.AllFileSizeInfo;
import you.fileserver.dto.FileDetail;
import you.fileserver.service.*;
import you.fileserver.utility.FileSizeLimitCalculator;

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
    private final ReportService reportService;
    private final FileDetailService fileDetailService;
    private final FileSizeLimitCalculator fileSizeLimitCalculator;

    public FileServerController(UploadService uploadService,
                                DownloadService DownloadService,
                                AuthService authService,
                                DeleteService deleteService,
                                ReportService reportService,
                                FileDetailService fileDetailService,
                                FileSizeLimitCalculator fileSizeLimitCalculator) {
        this.uploadService = uploadService;
        this.downloadService = DownloadService;
        this.authService = authService;
        this.deleteService = deleteService;
        this.reportService = reportService;
        this.fileDetailService = fileDetailService;
        this.fileSizeLimitCalculator = fileSizeLimitCalculator;
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
        return fileDetailService.fileDetails(userDetail);
    }

    //掲示板のすべてのファイルのサイズ合計とログインユーザーのファイル材図合計をかえすAPI(非ログイン時はログインユーザーファイルサイズは-1を返す)
    @GetMapping(ALL_FILE_DETAILS_PATH)
    @ResponseBody
    public AllFileSizeInfo getAllFileDetails(@AuthenticationPrincipal CustomUserDetails userDetail) {
        return fileDetailService.allFileDetails(userDetail);
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
    public ResponseEntity<?> downloadAuth(@RequestParam String targetFileName, @RequestParam String fileControlPassword, @AuthenticationPrincipal CustomUserDetails userDetail) {
        return downloadService.passwordAuth(targetFileName, fileControlPassword, userDetail);
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
    public Map<String, Object> delete(@RequestParam String targetFileName, @RequestParam String fileControlPassword, @AuthenticationPrincipal CustomUserDetails userDetail) {
        return deleteService.deleteFile(targetFileName, fileControlPassword, userDetail);
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

    //ユーザー登録できるかを返すAPI
    @GetMapping(ENABLE_CREATE_ACCOUNT_PATH)
    @ResponseBody
    public Map<String, Object> enableCreateAccount() {
        return fileSizeLimitCalculator.enableCreateAccount();
    }

    //ログイン中のユーザー名取得用API
    @GetMapping(USER_ID_PATH)
    @ResponseBody
    public Map<String, Object> getUser(@AuthenticationPrincipal CustomUserDetails userDetail) {
        if (userDetail == null) {
            return Map.of("user", "ゲスト");
        }
        return Map.of("user", userDetail.getUsername());
    }

    //問題報告用API
    @PostMapping(FILE_REPORT_PATH)
    @ResponseBody
    public Map<String, Object> fileReport(HttpServletRequest request, @RequestParam String targetFileName, @RequestParam String comment) {
        return reportService.saveReport(request, targetFileName, comment);
    }

}
