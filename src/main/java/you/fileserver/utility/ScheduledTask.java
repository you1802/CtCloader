package you.fileserver.utility;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import you.fileserver.repository.DownloadTokenInfoRepository;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ScheduledTask {
    DownloadTokenInfoRepository downloadTokenInfoRepository;

    /**
     * 定期実行メソッド(１時間に１回実行)
     */
    @Scheduled(fixedRate = 3600000)
    private void task() {
        downloadTokenInfoRepository.deleteByExpirationDateLessThan(LocalDateTime.now()); //期限切れトークンの削除
    }
}
