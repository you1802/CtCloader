package you.fileserver.security.annotation;

import org.springframework.stereotype.Component;

@Component
public class TestMethod {
    @IsAdmin
    void isAdminTest() {
        System.out.println("isAdminTest");
    }

    @IsUser
    void isUserTest() {
        System.out.println("isUserTest");
    }

}
