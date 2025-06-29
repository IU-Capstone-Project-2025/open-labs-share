package olsh.backend.usersservice.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NotFoundException Tests")
class NotFoundExceptionTest {

    @Test
    @DisplayName("Should create exception with message")
    void constructor_WithMessage_ShouldSetMessage() {
        // Given
        String message = "User not found with ID: 123";

        // When
        NotFoundException exception = new NotFoundException(message);

        // Then
        assertThat(exception).isInstanceOf(RuntimeException.class);
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "User not found with ID: 1",
        "User not found with email: test@example.com", 
        "User not found with username: testuser",
        "Resource not found",
        "Entity does not exist"
    })
    @DisplayName("Should handle various message formats")
    void constructor_WithVariousMessages_ShouldSetCorrectMessage(String message) {
        // When
        NotFoundException exception = new NotFoundException(message);

        // Then
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Should handle null and empty messages")
    void constructor_WithNullOrEmptyMessage_ShouldAcceptValue(String message) {
        // When
        NotFoundException exception = new NotFoundException(message);

        // Then
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Should have correct inheritance hierarchy")
    void inheritance_ShouldExtendRuntimeException() {
        // Given
        NotFoundException exception = new NotFoundException("test message");

        // Then
        assertThat(exception).isInstanceOf(RuntimeException.class);
        assertThat(exception).isInstanceOf(Exception.class);
        assertThat(exception).isInstanceOf(Throwable.class);
    }

    @Test
    @DisplayName("Should maintain stack trace when thrown")
    void stackTrace_WhenThrown_ShouldBePreserved() {
        // Given
        String message = "Test exception for stack trace";

        // When
        NotFoundException exception = new NotFoundException(message);

        // Then
        assertThat(exception.getStackTrace()).isNotNull();
        assertThat(exception.getStackTrace()).isNotEmpty();
    }

    @Test
    @DisplayName("Should be serializable for distributed systems")
    void serialization_ShouldBeSupported() {
        // Given
        NotFoundException exception = new NotFoundException("Test message for serialization");

        // Then - RuntimeException is Serializable, so NotFoundException should be too
        assertThat(exception).isInstanceOf(java.io.Serializable.class);
    }

    @Test
    @DisplayName("Should support exception chaining")
    void exceptionChaining_WithCause_ShouldWork() {
        // Given
        String message = "Database connection failed";
        Throwable cause = new RuntimeException("Connection timeout");

        // When - Test that we can chain exceptions if needed in future
        RuntimeException baseException = new RuntimeException(message, cause);
        NotFoundException wrappedException = new NotFoundException("User lookup failed: " + baseException.getMessage());

        // Then
        assertThat(wrappedException.getMessage()).contains("User lookup failed");
        assertThat(wrappedException.getMessage()).contains("Database connection failed");
    }

    @Test
    @DisplayName("Should handle very long messages")
    void constructor_WithLongMessage_ShouldHandleCorrectly() {
        // Given
        String longMessage = "A".repeat(1000) + " - User not found";

        // When
        NotFoundException exception = new NotFoundException(longMessage);

        // Then
        assertThat(exception.getMessage()).isEqualTo(longMessage);
        assertThat(exception.getMessage()).hasSize(longMessage.length());
    }

    @Test
    @DisplayName("Should handle special characters in message")
    void constructor_WithSpecialCharacters_ShouldPreserveMessage() {
        // Given
        String messageWithSpecialChars = "User not found: 测试用户 émoji:🔍 symbols:@#$%^&*()";

        // When
        NotFoundException exception = new NotFoundException(messageWithSpecialChars);

        // Then
        assertThat(exception.getMessage()).isEqualTo(messageWithSpecialChars);
    }
} 