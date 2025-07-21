package olsh.backend.usersservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.grpc.server.port=0", // Use random port to avoid conflicts
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class UsersServiceApplicationTests {

    @Test
    void contextLoads() {
        // Test passes if the Spring application context loads successfully
        // This verifies that all beans are correctly configured and wired
    }

}
