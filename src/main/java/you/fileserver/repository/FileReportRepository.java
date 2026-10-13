package you.fileserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import you.fileserver.dto.entity.FileReport;

@Repository
public interface FileReportRepository extends JpaRepository<FileReport, Long> {
}
