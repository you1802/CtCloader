package you.fileserver.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import you.fileserver.repository.UploadFileInfoRepository;

@SpringBootTest
class FileDetailServiceTest {
    @MockitoBean
    private UploadFileInfoRepository uploadFileInfoRepository;


    @Autowired
    private FileDetailService fileDetailService;

    /**
     * 権限テスト
     */
    //@IsAdmin
    @Test
    @WithMockUser(roles = "ADMIN")
    void adminFileDetailsTestWithAdmin() {
        fileDetailService.adminFileDetails();
    }

    @Test
    @WithMockUser(roles = "USER")
    void adminFileDetailsTestWithUser() {
        Assertions.assertThrows(AuthorizationDeniedException.class, () -> {
            fileDetailService.adminFileDetails();
        });
    }
}