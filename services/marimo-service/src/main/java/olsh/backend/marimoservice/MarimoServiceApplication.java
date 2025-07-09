package olsh.backend.marimoservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaRepositories
@EnableCaching
@EnableAsync
@EnableScheduling
public class MarimoServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MarimoServiceApplication.class, args);
    }
} 