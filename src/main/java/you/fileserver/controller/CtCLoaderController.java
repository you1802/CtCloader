package you.fileserver.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import static you.fileserver.config.Constants.*;

@Controller
public class CtCLoaderController {
    //ユーザーのすべての機能があるページ
    @GetMapping(UPLOADER_ROOT_PATH)
    public String ctcloader() {
        return "ctcloader";
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
