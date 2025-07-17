package olsh.backend.marimoservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "marimo")
@Data
public class MarimoProperties {

    private Session session = new Session();
    private Cache cache = new Cache();
    private PythonService pythonService = new PythonService();

    @Data
    public static class Session {
        private int ttlHours = 4;
        private int cleanupIntervalMinutes = 15;
        private int maxSessionsPerUser = 3;
    }

    @Data
    public static class Cache {
        private ComponentDefinitions componentDefinitions = new ComponentDefinitions();
        private ExecutionResults executionResults = new ExecutionResults();

        @Data
        public static class ComponentDefinitions {
            private int ttlHours = 2;
            private int maxSize = 500;
        }

        @Data
        public static class ExecutionResults {
            private int ttlMinutes = 15;
            private int maxSize = 1000;
        }
    }

    @Data
    public static class PythonService {
        private int maxRetries = 3;
        private int retryDelayMs = 1000;
        private int timeoutSeconds = 30;
    }
} 