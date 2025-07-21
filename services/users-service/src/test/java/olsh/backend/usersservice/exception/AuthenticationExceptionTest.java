package olsh.backend.usersservice.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AuthenticationException Tests")
class AuthenticationExceptionTest {

    @Test
    @DisplayName("Should create exception with message")
    void constructor_WithMessage_ShouldSetMessage() {
        // Given
        String message = "Invalid credentials";

        // When
        AuthenticationException exception = new AuthenticationException(message);

        // Then
        assertThat(exception).isInstanceOf(RuntimeException.class);
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "Invalid credentials",
        "Authentication failed",
        "Password mismatch",
        "User account locked",
        "Token expired",
        "Access denied",
        "Unauthorized access attempt"
    })
    @DisplayName("Should handle various authentication error messages")
    void constructor_WithVariousAuthMessages_ShouldSetCorrectMessage(String message) {
        // When
        AuthenticationException exception = new AuthenticationException(message);

        // Then
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Should handle null and empty messages")
    void constructor_WithNullOrEmptyMessage_ShouldAcceptValue(String message) {
        // When
        AuthenticationException exception = new AuthenticationException(message);

        // Then
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Should have correct inheritance hierarchy")
    void inheritance_ShouldExtendRuntimeException() {
        // Given
        AuthenticationException exception = new AuthenticationException("test message");

        // Then
        assertThat(exception).isInstanceOf(RuntimeException.class);
        assertThat(exception).isInstanceOf(Exception.class);
        assertThat(exception).isInstanceOf(Throwable.class);
    }

    @Test
    @DisplayName("Should maintain stack trace when thrown")
    void stackTrace_WhenThrown_ShouldBePreserved() {
        // Given
        String message = "Test authentication failure";

        // When
        AuthenticationException exception = new AuthenticationException(message);

        // Then
        assertThat(exception.getStackTrace()).isNotNull();
        assertThat(exception.getStackTrace()).isNotEmpty();
    }

    @Test
    @DisplayName("Should be serializable for distributed systems")
    void serialization_ShouldBeSupported() {
        // Given
        AuthenticationException exception = new AuthenticationException("Authentication failed for serialization test");

        // Then - RuntimeException is Serializable, so AuthenticationException should be too
        assertThat(exception).isInstanceOf(java.io.Serializable.class);
    }

    @Test
    @DisplayName("Should support security-sensitive message handling")
    void securitySensitiveMessages_ShouldBeHandledCorrectly() {
        // Given - Test that we can handle security-related messages properly
        String[] securityMessages = {
            "Invalid credentials", // Generic - good for security
            "Password incorrect", // More specific - could be security risk
            "User not found", // Could reveal user existence
            "Account locked due to multiple failed attempts"
        };

        for (String message : securityMessages) {
            // When
            AuthenticationException exception = new AuthenticationException(message);

            // Then
            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception).isInstanceOf(RuntimeException.class);
        }
    }

    @Test
    @DisplayName("Should handle authentication context information")
    void contextualAuthenticationErrors_ShouldPreserveDetails() {
        // Given
        String contextMessage = "Authentication failed for user: testuser at " + 
                               java.time.LocalDateTime.now() + " from IP: 192.168.1.1";

        // When
        AuthenticationException exception = new AuthenticationException(contextMessage);

        // Then
        assertThat(exception.getMessage()).isEqualTo(contextMessage);
        assertThat(exception.getMessage()).contains("testuser");
        assertThat(exception.getMessage()).contains("192.168.1.1");
    }

    @Test
    @DisplayName("Should be different from NotFoundException")
    void comparison_WithOtherExceptions_ShouldBeDifferentTypes() {
        // Given
        AuthenticationException authException = new AuthenticationException("Authentication failed");
        NotFoundException notFoundException = new NotFoundException("User not found");

        // Then
        assertThat(authException.getClass()).isNotEqualTo(notFoundException.getClass());
        assertThat(authException).isInstanceOf(RuntimeException.class);
        assertThat(notFoundException).isInstanceOf(RuntimeException.class);
        
        // Both should be runtime exceptions but different types
        assertThat(authException).isNotInstanceOf(NotFoundException.class);
        assertThat(notFoundException).isNotInstanceOf(AuthenticationException.class);
    }

    @Test
    @DisplayName("Should handle special characters in authentication messages")
    void constructor_WithSpecialCharacters_ShouldPreserveMessage() {
        // Given
        String messageWithSpecialChars = "Authentication failed: 用户认证失败 símbolos:@#$%^&*()";

        // When
        AuthenticationException exception = new AuthenticationException(messageWithSpecialChars);

        // Then
        assertThat(exception.getMessage()).isEqualTo(messageWithSpecialChars);
    }

    @Test
    @DisplayName("Should support exception chaining for authentication failures")
    void exceptionChaining_WithAuthenticationCause_ShouldWork() {
        // Given
        String message = "LDAP authentication failed";
        Throwable cause = new RuntimeException("LDAP server unreachable");

        // When - Test that we can chain authentication exceptions if needed in future
        RuntimeException baseException = new RuntimeException(message, cause);
        AuthenticationException wrappedException = new AuthenticationException("User authentication failed: " + baseException.getMessage());

        // Then
        assertThat(wrappedException.getMessage()).contains("User authentication failed");
        assertThat(wrappedException.getMessage()).contains("LDAP authentication failed");
    }
} 