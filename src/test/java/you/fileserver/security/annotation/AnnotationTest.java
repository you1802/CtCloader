package you.fileserver.security.annotation;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;

/**
 * 権限チェック用のアノテーションテスト
 */
@SpringBootTest
public class AnnotationTest {
    @Autowired
    private TestMethod testMethod;

    /*
    @IsAdminのテスト
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void isAdminTestWithAdmin() {
        testMethod.isAdminTest();
    }

    @Test
    @WithMockUser(roles = "USER")
    void isAdminTestWithUser() {
        Assertions.assertThrows(AuthorizationDeniedException.class, () -> testMethod.isAdminTest());
    }

    @Test
    @WithAnonymousUser
    void isAdminTestWithAnonymous() {
        Assertions.assertThrows(AuthorizationDeniedException.class, () -> testMethod.isAdminTest());
    }

    /*
    @IsUserのテスト
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void isUserTestWithAdmin() {
        Assertions.assertThrows(AuthorizationDeniedException.class, () -> testMethod.isUserTest());
    }

    @Test
    @WithMockUser(roles = "USER")
    void isUserTestWithUser() {
        testMethod.isUserTest();
    }

    @Test
    @WithAnonymousUser
    void isUserTestWithAnonymous() {
        Assertions.assertThrows(AuthorizationDeniedException.class, () -> testMethod.isUserTest());
    }
}
