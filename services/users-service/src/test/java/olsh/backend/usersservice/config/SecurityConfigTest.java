package olsh.backend.usersservice.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = SecurityConfig.class)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DisplayName("SecurityConfig Tests")
class SecurityConfigTest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Should create PasswordEncoder bean")
    void passwordEncoder_ShouldBeCreatedAsBean() {
        // Then
        assertThat(passwordEncoder).isNotNull();
        assertThat(passwordEncoder).isInstanceOf(BCryptPasswordEncoder.class);
    }

    @Test
    @DisplayName("Should encode passwords consistently")
    void passwordEncoder_ShouldEncodePasswordsConsistently() {
        // Given
        String plainPassword = "testPassword123";

        // When
        String encoded1 = passwordEncoder.encode(plainPassword);
        String encoded2 = passwordEncoder.encode(plainPassword);

        // Then
        assertThat(encoded1).isNotEqualTo(plainPassword);
        assertThat(encoded2).isNotEqualTo(plainPassword);
        assertThat(encoded1).isNotEqualTo(encoded2); // BCrypt uses salt, so each encoding is different
        
        // But both should match the original password
        assertThat(passwordEncoder.matches(plainPassword, encoded1)).isTrue();
        assertThat(passwordEncoder.matches(plainPassword, encoded2)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "password123",
        "StrongP@ssw0rd!",
        "simple",
        "verylongpasswordwithlotsofcharactersandsymbols!@#$%^&*()",
        "短密码",
        "émoji🔐password",
        "password with spaces",
        "UPPERCASE",
        "lowercase",
        "MiXeDcAsE123!"
    })
    @DisplayName("Should handle various password formats")
    void passwordEncoder_WithVariousPasswords_ShouldEncodeAndVerify(String password) {
        // When
        String encoded = passwordEncoder.encode(password);

        // Then
        assertThat(encoded).isNotEqualTo(password);
        assertThat(passwordEncoder.matches(password, encoded)).isTrue();
        assertThat(encoded).startsWith("$2a$"); // BCrypt prefix
    }

    @Test
    @DisplayName("Should not match incorrect passwords")
    void passwordEncoder_WithIncorrectPassword_ShouldNotMatch() {
        // Given
        String correctPassword = "correctPassword123";
        String incorrectPassword = "wrongPassword456";
        String encoded = passwordEncoder.encode(correctPassword);

        // When & Then
        assertThat(passwordEncoder.matches(incorrectPassword, encoded)).isFalse();
        assertThat(passwordEncoder.matches("", encoded)).isFalse();
        assertThatThrownBy(() -> passwordEncoder.matches(null, encoded))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should handle null and empty password scenarios safely")
    void passwordEncoder_WithNullOrEmptyInput_ShouldHandleSafely() {
        // Given
        String validPassword = "validPassword123";
        String encoded = passwordEncoder.encode(validPassword);

        // When & Then - BCrypt throws exception for null input, test for that behavior
        assertThatThrownBy(() -> passwordEncoder.matches(null, encoded))
                .isInstanceOf(IllegalArgumentException.class);
        
        assertThat(passwordEncoder.matches("", encoded)).isFalse();
    }

    @Test
    @DisplayName("Should use BCrypt with appropriate strength")
    void passwordEncoder_ShouldUseBCryptWithProperStrength() {
        // Given
        String password = "testPassword123";

        // When
        String encoded = passwordEncoder.encode(password);

        // Then
        assertThat(encoded).startsWith("$2a$"); // BCrypt format
        assertThat(encoded).hasSize(60); // Standard BCrypt hash length
        
        // Extract the cost factor (should be reasonable for security vs performance)
        String costPart = encoded.substring(4, 6);
        int cost = Integer.parseInt(costPart);
        assertThat(cost).isGreaterThanOrEqualTo(10); // Minimum reasonable cost
        assertThat(cost).isLessThanOrEqualTo(15); // Maximum reasonable cost for performance
    }

    @Test
    @DisplayName("Should be thread-safe for concurrent encoding")
    void passwordEncoder_ShouldBeThreadSafe() throws InterruptedException {
        // Given
        String password = "concurrentTestPassword";
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        String[] results = new String[threadCount];

        // When
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                results[index] = passwordEncoder.encode(password);
            });
            threads[i].start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        // Then
        for (int i = 0; i < threadCount; i++) {
            assertThat(results[i]).isNotNull();
            assertThat(passwordEncoder.matches(password, results[i])).isTrue();
        }
    }

    @Test
    @DisplayName("Should handle moderately long passwords")
    void passwordEncoder_WithLongPassword_ShouldHandleCorrectly() {
        // Given - Test with a reasonably long password (BCrypt limit is ~72 bytes)
        String longPassword = "ThisIsAVeryLongPasswordWithManyCharacters123!@#$%^&*()";

        // When
        String encoded = passwordEncoder.encode(longPassword);

        // Then
        assertThat(encoded).isNotNull();
        assertThat(passwordEncoder.matches(longPassword, encoded)).isTrue();
    }

    @Test
    @DisplayName("Should maintain security properties")
    void passwordEncoder_SecurityProperties_ShouldBeMaintained() {
        // Given
        String password1 = "password123";
        String password2 = "password124"; // Very similar password

        // When
        String encoded1 = passwordEncoder.encode(password1);
        String encoded2 = passwordEncoder.encode(password2);

        // Then
        assertThat(encoded1).isNotEqualTo(encoded2);
        assertThat(passwordEncoder.matches(password1, encoded1)).isTrue();
        assertThat(passwordEncoder.matches(password2, encoded2)).isTrue();
        
        // Cross-verification should fail
        assertThat(passwordEncoder.matches(password1, encoded2)).isFalse();
        assertThat(passwordEncoder.matches(password2, encoded1)).isFalse();
    }

    @Test
    @DisplayName("Should be compatible with Spring Security requirements")
    void passwordEncoder_ShouldBeCompatibleWithSpringSecurity() {
        // Given
        String password = "springSecurityTest";

        // When
        String encoded = passwordEncoder.encode(password);

        // Then
        assertThat(encoded).isNotNull();
        assertThat(encoded).isNotEmpty();
        assertThat(encoded).isNotEqualTo(password);
        
        // Should follow Spring Security PasswordEncoder contract
        assertThat(passwordEncoder.matches(password, encoded)).isTrue();
        
        // Should not match with different passwords
        assertThat(passwordEncoder.matches("differentPassword", encoded)).isFalse();
    }

    @Test
    @DisplayName("Should handle special authentication scenarios")
    void passwordEncoder_WithSpecialScenarios_ShouldHandleProperly() {
        // Given
        String[] specialPasswords = {
            "password with\ttabs",
            "password\nwith\nnewlines",
            "password\rwith\rcarriage\rreturns",
            "password with unicode: 测试密码",
            "password with emoji: 🔐🔑",
            "password; DROP TABLE users; --", // SQL injection attempt
            "<script>alert('xss')</script>", // XSS attempt
            "../../etc/passwd", // Path traversal attempt
        };

        for (String specialPassword : specialPasswords) {
            // When
            String encoded = passwordEncoder.encode(specialPassword);

            // Then
            assertThat(encoded).isNotNull();
            assertThat(passwordEncoder.matches(specialPassword, encoded)).isTrue();
        }
    }
} 