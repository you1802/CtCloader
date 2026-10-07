package you.fileserver.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import you.fileserver.dto.entity.DownloadTokenInfo;

import java.time.LocalDateTime;

@Repository
public interface DownloadTokenInfoRepository extends JpaRepository<DownloadTokenInfo,String> {

    //有効期限切れのトークン削除用
    @Transactional
    void deleteByExpirationDateLessThan(LocalDateTime now);
}
