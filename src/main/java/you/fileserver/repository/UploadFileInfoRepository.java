package you.fileserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import you.fileserver.dto.entity.UploadFileInfo;

import java.util.List;

@Repository
public interface UploadFileInfoRepository extends JpaRepository<UploadFileInfo,String> {
    public List<UploadFileInfo> findByOwnerOrVisible(String fileName, boolean visible);
}
