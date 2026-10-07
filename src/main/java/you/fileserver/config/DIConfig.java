package you.fileserver.config;

import me.desair.tus.server.TusFileUploadService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import static you.fileserver.config.Constants.TEMP_PATH;
import static you.fileserver.config.Constants.TUS_UPLOAD_PATH;

@Configuration
public class DIConfig {
    //TUSプロトコルを受け付けるライブラリ
    @Bean
    public TusFileUploadService tusFileUploadService() {
        return new TusFileUploadService()
                .withUploadUri(TUS_UPLOAD_PATH) //アップロード先のエンドポイントを指定
                .withStoragePath(TEMP_PATH); //一時ファイルの場所指定
    }

    //一定時間後に処理を実行するスケジューラー
    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        return scheduler;
    }
}
