package you.fileserver.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import me.desair.tus.server.exception.TusException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import you.fileserver.dto.*;
import you.fileserver.security.authentication.CustomUserDetails;
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
    private final LoginHistoryService loginHistoryService;

    public FileServerController(UploadService uploadService,
                                DownloadService DownloadService,
                                AuthService authService,
                                DeleteService deleteService,
                                ReportService reportService,
                                FileDetailService fileDetailService,
                                FileSizeLimitCalculator fileSizeLimitCalculator,
                                LoginHistoryService loginHistoryService) {
        this.uploadService = uploadService;
        this.downloadService = DownloadService;
        this.authService = authService;
        this.deleteService = deleteService;
        this.reportService = reportService;
        this.fileDetailService = fileDetailService;
        this.fileSizeLimitCalculator = fileSizeLimitCalculator;
        this.loginHistoryService = loginHistoryService;
    }

    //全ての機能があるページ
    @GetMapping(UPLOADER_ROOT_PATH)
    public String file() {
        return "ctcloader";
    }

    /*
     * テーブル関係
     */
    //掲示板のファイルリスト取得用API
    @GetMapping(FORUM_FILE_DETAILS_JSON_PATH)
    @ResponseBody
    public List<ForumFileDetail> fileDetails(@AuthenticationPrincipal CustomUserDetails userDetail) {
        return fileDetailService.getUserForumFileDetails(userDetail);
    }

    //対象ファイルの詳細情報取得API
    @GetMapping(TARGET_FILE_DETAIL)
    @ResponseBody
    public TargetFileDetail getTargetFileDetail(@RequestParam String fileID, @AuthenticationPrincipal CustomUserDetails userDetail) {
        return fileDetailService.getTargetFileDetail(fileID, userDetail);
    }

    //掲示板のすべてのファイルのサイズ合計とログインユーザーのファイル材図合計をかえすAPI(非ログイン時はログインユーザーファイルサイズは-1を返す)
    @GetMapping(ALL_FILESIZE_INFO_PATH)
    @ResponseBody
    public AllFileSizeInfo getAllFileDetails(@AuthenticationPrincipal CustomUserDetails userDetail) {
        return fileDetailService.allFileDetails(userDetail);
    }

    /*
     * ファイル操作
     */
    //アップロード用エンドポイント
    @RequestMapping(value = UPLOAD_PATH, method = {
            RequestMethod.POST, RequestMethod.PATCH, RequestMethod.HEAD, RequestMethod.DELETE, RequestMethod.OPTIONS
    })
    public void upload(HttpServletRequest request, HttpServletResponse response, @AuthenticationPrincipal CustomUserDetails userDetail) throws TusException, IOException {
        uploadService.upload(request, response, userDetail);
    }

    //ダウンロード用のトークン発行するAPI
    @PostMapping(DOWNLOAD_AUTH_PATH)
    public ResponseEntity<?> downloadAuth(@RequestParam String fileID, @RequestParam String fileControlPassword, @AuthenticationPrincipal CustomUserDetails userDetail, HttpServletRequest request) {
        return downloadService.passwordAuth(fileID, fileControlPassword, userDetail, request.getRemoteAddr());
    }

    //ファイル転送用URLを発行するAPI
    @PostMapping(TRANSFER_PATH)
    public ResponseEntity<?> transferUrl(@RequestParam String fileID, @AuthenticationPrincipal CustomUserDetails userDetail) {
        return downloadService.createFileTransferUrl(fileID, userDetail);
    }

    //ダウンロード用API
    @GetMapping(DOWNLOAD_PATH)
    public ResponseEntity<StreamingResponseBody> download(@RequestParam String fileID, @RequestParam String token, HttpServletRequest request) {
        return downloadService.downloadFile(fileID, token, request.getRemoteAddr());
    }

    //ファイル削除用API
    @PostMapping(DELETE_PATH)
    @ResponseBody
    public DefaultApiResponse delete(@RequestParam String fileID, @RequestParam String fileControlPassword, @AuthenticationPrincipal CustomUserDetails userDetail, HttpServletRequest request) {
        return deleteService.deleteFile(fileID, fileControlPassword, userDetail, request.getRemoteAddr());
    }

    //問題報告用API
    @PostMapping(FILE_REPORT_PATH)
    @ResponseBody
    public DefaultApiResponse fileReport(@RequestParam String fileID, @RequestParam String comment, HttpServletRequest request) {
        return reportService.saveReport(fileID, comment, request.getRemoteAddr());
    }

    /*
     * ユーザーアカウント制御
     */
    //ユーザー登録用API
    @PostMapping(REGISTER_PATH)
    @ResponseBody
    public DefaultApiResponse register(@RequestParam String username, @RequestParam String password) {
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
        return authService.getUser(userDetail);
    }

    //ユーザー削除用API
    @PostMapping(DELETE_USER_PATH)
    @ResponseBody
    public Map<String, Object> deleteUser(HttpServletRequest request, HttpServletResponse response, CsrfToken csrfToken, @AuthenticationPrincipal CustomUserDetails userDetail) {
        authService.deleteUser(userDetail.getUsername());

        //ログアウト処理
        SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();
        logoutHandler.logout(request, response, null);

        return Map.of(
                "code", 200,
                "message", "ユーザーの削除に成功しました。",
                "csrfToken", csrfToken.getToken()
        );
    }

    //ユーザーパスワード変更用API
    @PostMapping(USER_PASSWORD_CHANGE_PATH)
    @ResponseBody
    public DefaultApiResponse userPasswordChange(@RequestParam String oldPassword, @RequestParam String newPassword, @AuthenticationPrincipal CustomUserDetails userDetail) {
        return authService.changePassword(oldPassword, newPassword, userDetail);
    }

    /*
     * マイページ関連
     */
    //ログイン中ユーザー自身が、アップロードしたファイルのみの詳細リストを取得するAPI(ログイン時専用)
    @GetMapping(OWNED_FILE_DETAILS_JSON_PATH)
    @ResponseBody
    public List<FileDetail> ownedFileDetails(@AuthenticationPrincipal CustomUserDetails userDetail) {
        return fileDetailService.userOwnedFileDetails(userDetail);
    }

    //ログイン履歴を取得するAPI
    @GetMapping(LOGIN_HISTORY_PATH)
    @ResponseBody
    public List<UserLoginHistory> loginHistory(@AuthenticationPrincipal CustomUserDetails userDetail) {
        return loginHistoryService.getLoginHistory(userDetail);
    }
}
