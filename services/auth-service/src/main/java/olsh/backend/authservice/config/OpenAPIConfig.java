package olsh.backend.authservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                      .title("Authentication Service API")
                      .version("1.0.0")
                      .description(
                          "API for user authentication, registration, and password management.")
            );
    }
}
