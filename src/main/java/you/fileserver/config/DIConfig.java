package you.fileserver.config;

import me.desair.tus.server.TusFileUploadService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static you.fileserver.config.Constants.TEMP_PATH;
import static you.fileserver.config.Constants.TUS_UPLOAD_PATH;

@Configuration
public class DIConfig {
    @Bean
    public TusFileUploadService tusFileUploadService() {
        return new TusFileUploadService()
                .withUploadUri(TUS_UPLOAD_PATH)
                .withStoragePath(TEMP_PATH);
    }
}
