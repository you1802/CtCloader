package you.fileserver.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import you.fileserver.entity.FileDetail;
import you.fileserver.service.UploaderService;

import java.util.List;

import static you.fileserver.config.Constants.FILE_DETAILS_JSON_PATH;
import static you.fileserver.config.Constants.UPLOADER_ROOT_PATH;

@Controller
public class FileServerController {
    UploaderService uploaderService;

    public FileServerController(UploaderService uploaderService) {
        this.uploaderService = uploaderService;
    }

    @GetMapping(UPLOADER_ROOT_PATH)
    public String file() {
        return "uploader";
    }

    @GetMapping(FILE_DETAILS_JSON_PATH)
    @ResponseBody
    public List<FileDetail> fileDetails() {
        return uploaderService.fileDetails();
    }
}
