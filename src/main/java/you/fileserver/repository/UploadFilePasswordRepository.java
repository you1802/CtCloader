package you.fileserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import you.fileserver.dto.entity.UploadFilePassword;

@Repository
public interface UploadFilePasswordRepository extends JpaRepository<UploadFilePassword, String> {
}
