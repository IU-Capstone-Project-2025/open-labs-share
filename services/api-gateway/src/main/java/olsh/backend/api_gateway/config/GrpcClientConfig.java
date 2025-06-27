package olsh.backend.api_gateway.config;


import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class GrpcClientConfig {

    public GrpcClientConfig() {
        log.info("gRPC client configuration initialized");
    }
}

