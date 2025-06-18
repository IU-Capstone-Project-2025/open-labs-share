package olsh.backend.authservice;

import org.springframework.boot.SpringApplication;

public class TestAuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.from(AuthServiceApplication::main).with(TestcontainersConfiguration.class)
            .run(args);
    }

}
