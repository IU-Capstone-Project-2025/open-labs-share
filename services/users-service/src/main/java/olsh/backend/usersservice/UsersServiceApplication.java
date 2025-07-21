package olsh.backend.usersservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import olsh.backend.usersservice.config.PointsConfig;

@SpringBootApplication
@EnableConfigurationProperties(PointsConfig.class)
public class UsersServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UsersServiceApplication.class, args);
    }

}
