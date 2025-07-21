package olsh.backend.api_gateway.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "grpc.upload")
@Component
@Data
public class UploadFileConfiguration {
    private int chunkSize = 8192; // 8KB default
    private int timeoutSeconds = 120;
    private long maxFileSize = 100 * 1024 * 1024; // 100MB default
}

