package you.fileserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import you.fileserver.dto.AdminUserDetail;
import you.fileserver.dto.entity.UserAccount;

import java.util.List;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount, String> {
    @Query("""
            SELECT new you.fileserver.dto.AdminUserDetail(
            u.username,
            MAX(l.loginAt)
            )
            FROM UserAccount u
            LEFT JOIN LoginHistory l ON l.username = u.username
            GROUP BY u.username, l.username
            """)
    List<AdminUserDetail> findAllWithLastLoginAt();
}
