package you.fileserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import you.fileserver.dto.entity.UserAccount;

public interface UserAccountRepository extends JpaRepository<UserAccount, String> {
}
