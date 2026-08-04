package you.fileserver.service;

import org.springframework.stereotype.Service;
import you.fileserver.entity.FileDetail;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class UploaderService {

    //テスト用
    public List<FileDetail> fileDetails() {
        List<FileDetail> fileDetails = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            fileDetails.add(FileDetail.builder()
                    .name("ファイル" + i)
                    .size(123456 * i)
                    .owner(i % 2 == 0)
                    .lastModifiedDate(LocalDateTime.now().minusDays(i)).
                    build());
        }
        return fileDetails;
    }
}
