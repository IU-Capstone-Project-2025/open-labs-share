package olsh.backend.api_gateway.service;


import olsh.backend.api_gateway.dto.response.UserResponse;
import olsh.backend.api_gateway.exception.UserNotFoundException;
import olsh.backend.api_gateway.grpc.client.UserServiceClient;
import olsh.backend.api_gateway.grpc.proto.UsersServiceProto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("UserService - User Retrieval and Mapping Tests")
class UserServiceTest {

    private UserService userService;
    private UserServiceClient userServiceClient;

    @BeforeEach
    @DisplayName("Set up test environment with mocked dependencies")
    void setUp() {
        // Create mock for the gRPC client
        userServiceClient = mock(UserServiceClient.class);

        // Create REAL service instance
        userService = new UserService(userServiceClient);

        System.out.println("Setting up UserService tests...");
    }

    @AfterEach
    @DisplayName("Clean up test environment")
    void tearDown() {
        System.out.println("Cleaning up UserService tests...");
        userService = null;
    }

    // Test 1: Successful user retrieval and mapping
    @Test
    @DisplayName("Should successfully retrieve and map user data when user exists")
    void getUserById_UserExists_ReturnsCorrectUserResponse() {
        // Given
        Long userId = 123L;
        UsersServiceProto.UserInfo mockUser = createFoundUser(
                userId, "johndoe", "John", "Doe", "john.doe@example.com"
        );
        when(userServiceClient.getUser(userId)).thenReturn(mockUser);

        // When
        UserResponse result = userService.getUserById(userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(userId);
        assertThat(result.getUsername()).isEqualTo("johndoe");
        assertThat(result.getName()).isEqualTo("John");
        assertThat(result.getSurname()).isEqualTo("Doe");
        assertThat(result.getEmail()).isEqualTo("john.doe@example.com");

        verify(userServiceClient).getUser(userId);
    }

    @Test
    @DisplayName("Should map all user data fields correctly")
    void getUserById_UserExists_MapsAllFieldsCorrectly() {
        // Given
        Long userId = 456L;
        String username = "janedoe";
        String name = "Jane";
        String surname = "Doe";
        String email = "jane.doe@company.org";

        UsersServiceProto.UserInfo mockUserData = createFoundUser(userId, username, name, surname, email);
        when(userServiceClient.getUser(userId)).thenReturn(mockUserData);

        // When
        UserResponse result = userService.getUserById(userId);

        // Then - Verify each field is mapped correctly
        assertThat(result.getId()).isEqualTo(userId);
        assertThat(result.getUsername()).isEqualTo(username);
        assertThat(result.getName()).isEqualTo(name);
        assertThat(result.getSurname()).isEqualTo(surname);
        assertThat(result.getEmail()).isEqualTo(email);
    }

    // Test 2: User not found scenarios
    @Test
    @DisplayName("Should throw UserNotFoundException when user is not found")
    void getUserById_UserNotFound_ThrowsUserNotFoundException() {
        // Given
        Long nonExistentUserId = 999L;
        when(userServiceClient.getUser(nonExistentUserId)).thenThrow(new UserNotFoundException("User not found with id: " + nonExistentUserId));

        // When & Then
        assertThatThrownBy(() -> userService.getUserById(nonExistentUserId))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found with id: " + nonExistentUserId);

        verify(userServiceClient).getUser(nonExistentUserId);
    }

    @Test
    @DisplayName("Should include correct user ID in exception message")
    void getUserById_UserNotFound_IncludesCorrectUserIdInMessage() {
        // Given
        Long userId = 42L;
        when(userServiceClient.getUser(userId)).thenThrow(new UserNotFoundException("User not found with id: " + userId));

        // When & Then
        assertThatThrownBy(() -> userService.getUserById(userId))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found with id: " + userId);
    }

    // Test 3: Parameterized tests for various user IDs
    @ParameterizedTest(name = "Should successfully retrieve user with ID: {0}")
    @ValueSource(longs = {1L, 100L, 999L, 12345L, Long.MAX_VALUE})
    @DisplayName("User retrieval with various valid user IDs")
    void getUserById_VariousValidUserIds_ReturnsUserResponse(Long userId) {
        // Given
        UsersServiceProto.UserInfo mockUserData = createFoundUser(userId, "user" + userId, "Name", "Surname", "user" + userId + "@test" +
                ".com");
        when(userServiceClient.getUser(userId)).thenReturn(mockUserData);

        // When
        UserResponse result = userService.getUserById(userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(userId);
        assertThat(result.getUsername()).isEqualTo("user" + userId);

        verify(userServiceClient).getUser(userId);
    }

    @ParameterizedTest(name = "Should throw UserNotFoundException for non-existent user ID: {0}")
    @ValueSource(longs = {0L, -1L, 404L, 999999L})
    @DisplayName("User not found scenarios with various user IDs")
    void getUserById_VariousNonExistentUserIds_ThrowsUserNotFoundException(Long userId) {
        // Given
        when(userServiceClient.getUser(userId)).thenThrow(new UserNotFoundException("User not found with id: " + userId));

        // When & Then
        assertThatThrownBy(() -> userService.getUserById(userId))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found with id: " + userId);
    }

    // Test 4: Edge cases with user data fields
    @ParameterizedTest(name = "Should handle user data: {0}")
    @MethodSource("provideUserDataVariations")
    @DisplayName("User data mapping with various field values")
    void getUserById_VariousUserDataFields_MapsCorrectly(UserDataTestCase testCase) {
        // Given
        UsersServiceProto.UserInfo mockUserData = createFoundUser(
                testCase.userId,
                testCase.username,
                testCase.name,
                testCase.surname,
                testCase.email
        );
        when(userServiceClient.getUser(testCase.userId)).thenReturn(mockUserData);

        // When
        UserResponse result = userService.getUserById(testCase.userId);

        // Then
        assertThat(result.getId()).isEqualTo(testCase.userId);
        assertThat(result.getUsername()).isEqualTo(testCase.username);
        assertThat(result.getName()).isEqualTo(testCase.name);
        assertThat(result.getSurname()).isEqualTo(testCase.surname);
        assertThat(result.getEmail()).isEqualTo(testCase.email);
    }

    // Data provider for various user data scenarios
    static Stream<UserDataTestCase> provideUserDataVariations() {
        return Stream.of(
                new UserDataTestCase(1L, "john", "John", "Doe", "john@example.com"),
                new UserDataTestCase(2L, "jane_doe", "Jane", "Doe-Smith", "jane.doe@company.org"),
                new UserDataTestCase(3L, "user123", "María", "García", "maria.garcia@email.es"),
                new UserDataTestCase(4L, "admin", "Admin", "User", "admin@system.local"),
                new UserDataTestCase(5L, "test.user", "Test", "User", "test.user+tag@domain.co.uk"),
                new UserDataTestCase(6L, "user_with_long_name", "Very Long First Name", "Very Long Last Name",
                        "verylongemail@verylongdomain.example.com"),
                new UserDataTestCase(7L, "u", "A", "B", "a@b.c"), // Minimal values
                new UserDataTestCase(8L, "user.with.dots", "Name With Spaces", "Surname-With-Dashes", "email+with" +
                        "+plus@domain.org")
        );
    }

    // Test case class for parameterized testing
    static class UserDataTestCase {
        final Long userId;
        final String username;
        final String name;
        final String surname;
        final String email;

        UserDataTestCase(Long userId, String username, String name, String surname, String email) {
            this.userId = userId;
            this.username = username;
            this.name = name;
            this.surname = surname;
            this.email = email;
        }

        @Override
        public String toString() {
            return String.format("UserData{id=%d, username='%s', name='%s', surname='%s', email='%s'}",
                    userId, username, name, surname, email);
        }
    }

    // Test 5: Exception handling and error messages
    @Test
    @DisplayName("Should throw exactly UserNotFoundException (not subclass)")
    void getUserById_UserNotFound_ThrowsCorrectExceptionType() {
        // Given
        Long userId = 123L;
        when(userServiceClient.getUser(userId)).thenThrow(new UserNotFoundException("User not found with id: " + userId));

        // When & Then
        assertThatThrownBy(() -> userService.getUserById(userId))
                .isExactlyInstanceOf(UserNotFoundException.class)
                .hasNoCause();
    }

    @Test
    @DisplayName("Should provide consistent error messages for user not found scenarios")
    void getUserById_MultipleUserNotFoundScenarios_ConsistentErrorMessages() {
        // Given
        Long[] nonExistentUserIds = {100L, 200L, 300L};

        // When & Then
        for (Long userId : nonExistentUserIds) {
            when(userServiceClient.getUser(userId)).thenThrow(new UserNotFoundException("User not found with id: " + userId));

            assertThatThrownBy(() -> userService.getUserById(userId))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessage("User not found with id: " + userId);
        }
    }

    // Test 6: gRPC client interaction verification
    @Test
    @DisplayName("Should call gRPC client exactly once per request")
    void getUserById_ValidRequest_CallsGrpcClientOnce() {
        // Given
        Long userId = 123L;
        UsersServiceProto.UserInfo mockUserData = createFoundUser(userId, "test", "Test", "User", "test@example.com");
        when(userServiceClient.getUser(userId)).thenReturn(mockUserData);

        // When
        userService.getUserById(userId);

        // Then
        verify(userServiceClient, times(1)).getUser(userId);
        verifyNoMoreInteractions(userServiceClient);
    }

    @Test
    @DisplayName("Should not call gRPC client multiple times for same request")
    void getUserById_MultipleCallsSameUser_CallsGrpcClientEachTime() {
        // Given
        Long userId = 123L;
        UsersServiceProto.UserInfo mockUserData = createFoundUser(userId, "test", "Test", "User", "test@example.com");
        when(userServiceClient.getUser(userId)).thenReturn(mockUserData);

        // When
        userService.getUserById(userId);
        userService.getUserById(userId);

        // Then
        verify(userServiceClient, times(2)).getUser(userId);
    }

    // Test 7: gRPC client exception propagation
    @Test
    @DisplayName("Should propagate gRPC client exceptions")
    void getUserById_GrpcClientThrowsException_PropagatesException() {
        // Given
        Long userId = 123L;
        RuntimeException grpcException = new RuntimeException("gRPC connection failed");
        when(userServiceClient.getUser(userId)).thenThrow(grpcException);

        // When & Then
        assertThatThrownBy(() -> userService.getUserById(userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("gRPC connection failed")
                .isEqualTo(grpcException);

        verify(userServiceClient).getUser(userId);
    }

    // Test 8: Null user ID handling (edge case)
    @Test
    @DisplayName("Should handle null user ID by passing it to gRPC client")
    void getUserById_NullUserId_PassesToGrpcClient() {
        // Given
        Long nullUserId = null;
        when(userServiceClient.getUser(nullUserId)).thenThrow(new UserNotFoundException("User not found with id: null"));

        // When & Then
        assertThatThrownBy(() -> userService.getUserById(nullUserId))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found with id: null");

        verify(userServiceClient).getUser(nullUserId);
    }

    // Test 9: Response object immutability verification
    @Test
    @DisplayName("Should create new UserResponse instance for each call")
    void getUserById_MultipleCalls_CreatesNewResponseInstances() {
        // Given
        Long userId = 123L;
        UsersServiceProto.UserInfo mockUserData = createFoundUser(userId, "test", "Test", "User", "test@example.com");
        when(userServiceClient.getUser(userId)).thenReturn(mockUserData);

        // When
        UserResponse response1 = userService.getUserById(userId);
        UserResponse response2 = userService.getUserById(userId);

        // Then
        assertThat(response1).isNotSameAs(response2); // Different instances
        assertThat(response1.getId()).isEqualTo(response2.getId()); // Same content
        assertThat(response1.getUsername()).isEqualTo(response2.getUsername());
    }

    // Helper methods to create test data
    private UsersServiceProto.UserInfo createFoundUser(Long id, String username, String name, String surname,
                                                      String email) {
        return createFoundUser(id, username, name, surname, email, 0, 0, 0);
    }

    private UsersServiceProto.UserInfo createFoundUser(Long id, String username, String name, String surname,
                                                      String email, Integer labsSolved, Integer labsReviewed,
                                                      Integer balance) {
        return UsersServiceProto.UserInfo.newBuilder()
                .setUserId(id)
                .setUsername(username)
                .setFirstName(name)
                .setLastName(surname)
                .setEmail(email)
                .setLabsSolved(labsSolved)
                .setLabsReviewed(labsReviewed)
                .setBalance(balance)
                .build();
    }

    private UsersServiceProto.UserInfo createNotFoundUserData() {
        throw new UserNotFoundException("User not found");
    }
}
