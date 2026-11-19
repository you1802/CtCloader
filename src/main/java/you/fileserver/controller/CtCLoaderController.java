package you.fileserver.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import you.fileserver.dto.DownloadBridgePageInfo;
import you.fileserver.service.DownloadService;

import static you.fileserver.config.Constants.*;

@Controller
@RequiredArgsConstructor
public class CtCLoaderController {
    private final DownloadService downloadService;

    //ユーザーのすべての機能があるページ
    @GetMapping(UPLOADER_ROOT_PATH)
    public String ctcloader() {
        return "ctcloader";
    }

    //転送用クッションページ
    @GetMapping(DOWNLOAD_BRIDGE_PAGE_PATH)
    public String downloadBridgesPage(@RequestParam String fileID, @RequestParam String token, Model model) {
        DownloadBridgePageInfo pageInfo = downloadService.getTransferPageInfo(fileID, token);
        model.addAttribute("pageInfo", pageInfo);
        return "download";
    }

    /*
    ADMIN関係
     */
    //ADMIN用機能がまとまったページ
    @GetMapping(ADMIN_HOME_PATH)
    public String adminHome() {
        return "admin/home";
    }

    //ADMIN用ログイン画面
    @GetMapping(ADMIN_LOGIN_PATH)
    public String adminLogin() {
        return "admin/login";
    }
}
