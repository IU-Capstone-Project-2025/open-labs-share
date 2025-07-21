package olsh.backend.api_gateway.service;

import olsh.backend.api_gateway.exception.AuthenticationException;
import olsh.backend.api_gateway.grpc.client.AuthServiceClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@DisplayName("AuthService - Token Validation Tests")
class AuthServiceTest {

    private AuthService authService;
    private AuthServiceClient authServiceClient;

    @BeforeEach
    @DisplayName("Set up test environment with mocked dependencies")
    void setUp() {
        authServiceClient = mock(AuthServiceClient.class);
        authService = new AuthService(authServiceClient);
        System.out.println("Setting up AuthService tests...");
    }

    @AfterEach
    @DisplayName("Clean up test environment")
    void tearDown() {
        System.out.println("Cleaning up AuthService tests...");
        authService = null;
    }

    // Test 1: Null and empty tokens using @NullAndEmptySource
    @ParameterizedTest(name = "Should throw AuthenticationException when token is: [{0}]")
    @NullAndEmptySource
    @DisplayName("Validate Token - Null and Empty Token Scenarios")
    void validateToken_NullAndEmptyTokens_ThrowsAuthenticationException(String invalidToken) {
        // When & Then
        assertThatThrownBy(() -> authService.validateToken(invalidToken))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Token is required");
    }

    // Test 2: All whitespace variations using @MethodSource (consolidated)
    @ParameterizedTest(name = "Should throw AuthenticationException for whitespace-only token: [{0}]")
    @MethodSource("provideAllWhitespaceVariations")
    @DisplayName("Validate Token - All Whitespace Variations")
    void validateToken_AllWhitespaceVariations_ThrowsAuthenticationException(String whitespaceToken) {
        // When & Then
        assertThatThrownBy(() -> authService.validateToken(whitespaceToken))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Token is required");
    }

    // Consolidated method with ALL whitespace variations
    static Stream<String> provideAllWhitespaceVariations() {
        String[] allWhitespaceTokens = {
                // Basic whitespace
                " ",                    // Single space
                "   ",                  // Multiple spaces
                "     ",                // Even more spaces

                // Tab characters
                "\t",                   // Single tab
                "\t\t",                 // Multiple tabs

                // Newline characters
                "\n",                   // Newline
                "\n\n",                 // Multiple newlines

                // Other ASCII whitespace
                "\r",                   // Carriage return
                "\f",                   // Form feed

                // Unicode whitespace characters
                "\u2000",               // En quad
                "\u2001",               // Em quad
                "\u2002",               // En space
                "\u2028",               // Line separator
                "\u2029",               // Paragraph separator

                // Mixed whitespace combinations
                " \t ",                 // Mixed spaces and tabs
                " \n\r ",               // Complex whitespace mix
                "  \t\n\r  ",           // Multiple different whitespace characters
                "\u2000\u2001\u2002",   // Multiple Unicode whitespace
        };
        return Stream.of(allWhitespaceTokens);
    }
}
