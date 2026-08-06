package you.fileserver.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import me.desair.tus.server.exception.TusException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import you.fileserver.entity.FileDetail;
import you.fileserver.service.UploaderService;

import java.io.IOException;
import java.util.List;

import static you.fileserver.config.Constants.*;

@Controller
public class FileServerController {
    UploaderService uploaderService;

    public FileServerController(UploaderService uploaderService) {
        this.uploaderService = uploaderService;
    }

    //全ての機能があるページ
    @GetMapping(UPLOADER_ROOT_PATH)
    public String file() {
        return "uploader";
    }


    //ファイルリスト取得用API
    @GetMapping(FILE_DETAILS_JSON_PATH)
    @ResponseBody
    public List<FileDetail> fileDetails() {
        return uploaderService.fileDetails();
    }

    //アップロード用エンドポイント
    @RequestMapping(value = UPLOAD_PATH, method = {
            RequestMethod.POST, RequestMethod.PATCH, RequestMethod.HEAD, RequestMethod.DELETE, RequestMethod.OPTIONS
    })
    public void upload(HttpServletRequest request, HttpServletResponse response) throws TusException, IOException {
        uploaderService.upload(request, response);
    }

    //ダウンロード用エンドポイント
    @GetMapping(DOWNLOAD_PATH)
    public ResponseEntity<StreamingResponseBody> download(@RequestParam String uniqueFileName) {
        return uploaderService.download(uniqueFileName);
    }

    @PostMapping(DELETE_PATH)
    public void delete(@RequestParam String uniqueFileName) {
        System.out.println(uploaderService.deleteFile(uniqueFileName));
    }
}
