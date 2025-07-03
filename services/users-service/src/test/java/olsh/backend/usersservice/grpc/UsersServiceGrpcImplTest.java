package olsh.backend.usersservice.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.olsh.users.proto.AuthenticateUserRequest;
import com.olsh.users.proto.CreateUserRequest;
import com.olsh.users.proto.DeleteUserRequest;
import com.olsh.users.proto.DeleteUserResponse;
import com.olsh.users.proto.ExistsResponse;
import com.olsh.users.proto.FindUserByEmailRequest;
import com.olsh.users.proto.FindUserByUsernameRequest;
import com.olsh.users.proto.GetUserInfoRequest;
import com.olsh.users.proto.GetUserProfileRequest;
import com.olsh.users.proto.HealthCheckRequest;
import com.olsh.users.proto.HealthCheckResponse;
import com.olsh.users.proto.SearchUsersRequest;
import com.olsh.users.proto.SearchUsersResponse;
import com.olsh.users.proto.UpdatePasswordRequest;
import com.olsh.users.proto.UpdatePasswordResponse;
import com.olsh.users.proto.UpdateUserLastLoginRequest;
import com.olsh.users.proto.UpdateUserLastLoginResponse;
import com.olsh.users.proto.UpdateUserProfileRequest;
import com.olsh.users.proto.UserInfo;
import com.olsh.users.proto.UserInfoResponse;
import com.olsh.users.proto.UserProfileResponse;

import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.stub.StreamObserver;
import olsh.backend.usersservice.exception.AuthenticationException;
import olsh.backend.usersservice.exception.NotFoundException;
import olsh.backend.usersservice.service.UserService;

@ExtendWith(MockitoExtension.class)
@DisplayName("UsersServiceGrpcImpl Tests")
class UsersServiceGrpcImplTest {

    @Mock
    private UserService userService;
    
    @Mock
    private StreamObserver<UserProfileResponse> userProfileResponseObserver;
    
    @Mock
    private StreamObserver<UserInfoResponse> userInfoResponseObserver;
    
    @Mock
    private StreamObserver<SearchUsersResponse> searchUsersResponseObserver;
    
    @Mock
    private StreamObserver<UpdatePasswordResponse> updatePasswordResponseObserver;
    
    @Mock
    private StreamObserver<UpdateUserLastLoginResponse> updateUserLastLoginResponseObserver;
    
    @Mock
    private StreamObserver<HealthCheckResponse> healthCheckResponseObserver;
    
    @Mock
    private StreamObserver<ExistsResponse> existsResponseObserver;
    
    @Mock
    private StreamObserver<DeleteUserResponse> deleteUserResponseObserver;

    private UsersServiceGrpcImpl grpcService;

    @BeforeEach
    void setUp() {
        grpcService = new UsersServiceGrpcImpl(userService);
    }

    // =============== getUserProfile Tests ===============

    @Test
    @DisplayName("Should get user profile successfully")
    void getUserProfile_WithValidRequest_ShouldReturnProfile() {
        // Given
        GetUserProfileRequest request = GetUserProfileRequest.newBuilder()
                .setUserId(1L)
                .build();
        
        UserProfileResponse expectedResponse = UserProfileResponse.newBuilder()
                .setUserInfo(UserInfo.newBuilder()
                        .setUserId(1L)
                        .setUsername("testuser")
                        .setEmail("test@example.com")
                        .build())
                .build();
        
        when(userService.getUserProfile(request)).thenReturn(expectedResponse);

        // When
        grpcService.getUserProfile(request, userProfileResponseObserver);

        // Then
        verify(userService).getUserProfile(request);
        verify(userProfileResponseObserver).onNext(expectedResponse);
        verify(userProfileResponseObserver).onCompleted();
        verify(userProfileResponseObserver, never()).onError(any());
    }

    @Test
    @DisplayName("Should handle user not found in getUserProfile")
    void getUserProfile_WithNonExistentUser_ShouldReturnNotFound() {
        // Given
        GetUserProfileRequest request = GetUserProfileRequest.newBuilder()
                .setUserId(999L)
                .build();
        
        when(userService.getUserProfile(request))
                .thenThrow(new NotFoundException("User not found with ID: 999"));

        // When
        grpcService.getUserProfile(request, userProfileResponseObserver);

        // Then
        ArgumentCaptor<StatusException> exceptionCaptor = ArgumentCaptor.forClass(StatusException.class);
        verify(userProfileResponseObserver).onError(exceptionCaptor.capture());
        
        StatusException exception = exceptionCaptor.getValue();
        assertThat(exception.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
        assertThat(exception.getStatus().getDescription()).contains("User not found with ID: 999");
        
        verify(userProfileResponseObserver, never()).onNext(any());
        verify(userProfileResponseObserver, never()).onCompleted();
    }

    @Test
    @DisplayName("Should handle internal error in getUserProfile")
    void getUserProfile_WithInternalError_ShouldReturnInternalError() {
        // Given
        GetUserProfileRequest request = GetUserProfileRequest.newBuilder()
                .setUserId(1L)
                .build();
        
        when(userService.getUserProfile(request))
                .thenThrow(new RuntimeException("Database connection failed"));

        // When
        grpcService.getUserProfile(request, userProfileResponseObserver);

        // Then
        ArgumentCaptor<StatusException> exceptionCaptor = ArgumentCaptor.forClass(StatusException.class);
        verify(userProfileResponseObserver).onError(exceptionCaptor.capture());
        
        StatusException exception = exceptionCaptor.getValue();
        assertThat(exception.getStatus().getCode()).isEqualTo(Status.Code.INTERNAL);
        assertThat(exception.getStatus().getDescription()).contains("Internal server error");
    }

    // =============== updateUserProfile Tests ===============

    @Test
    @DisplayName("Should update user profile successfully")
    void updateUserProfile_WithValidRequest_ShouldReturnUpdatedProfile() {
        // Given
        UpdateUserProfileRequest request = UpdateUserProfileRequest.newBuilder()
                .setUserId(1L)
                .setFirstName("John")
                .setLastName("Doe")
                .build();
        
        UserProfileResponse expectedResponse = UserProfileResponse.newBuilder()
                .setUserInfo(UserInfo.newBuilder()
                        .setUserId(1L)
                        .setFirstName("John")
                        .setLastName("Doe")
                        .build())
                .build();
        
        when(userService.updateUserProfile(request)).thenReturn(expectedResponse);

        // When
        grpcService.updateUserProfile(request, userProfileResponseObserver);

        // Then
        verify(userService).updateUserProfile(request);
        verify(userProfileResponseObserver).onNext(expectedResponse);
        verify(userProfileResponseObserver).onCompleted();
    }

    @Test
    @DisplayName("Should handle invalid argument in updateUserProfile")
    void updateUserProfile_WithInvalidArgument_ShouldReturnInvalidArgument() {
        // Given
        UpdateUserProfileRequest request = UpdateUserProfileRequest.newBuilder()
                .setUserId(1L)
                .setEmail("invalid-email")
                .build();
        
        when(userService.updateUserProfile(request))
                .thenThrow(new IllegalArgumentException("Invalid email format"));

        // When
        grpcService.updateUserProfile(request, userProfileResponseObserver);

        // Then
        ArgumentCaptor<StatusException> exceptionCaptor = ArgumentCaptor.forClass(StatusException.class);
        verify(userProfileResponseObserver).onError(exceptionCaptor.capture());
        
        StatusException exception = exceptionCaptor.getValue();
        assertThat(exception.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
        assertThat(exception.getStatus().getDescription()).contains("Invalid email format");
    }

    // =============== searchUsers Tests ===============

    @Test
    @DisplayName("Should search users successfully")
    void searchUsers_WithValidQuery_ShouldReturnResults() {
        // Given
        SearchUsersRequest request = SearchUsersRequest.newBuilder()
                .setQuery("john")
                .setPage(0)
                .setSize(10)
                .build();
        
        SearchUsersResponse expectedResponse = SearchUsersResponse.newBuilder()
                .addUsers(UserInfo.newBuilder()
                        .setUserId(1L)
                        .setUsername("john_doe")
                        .setFirstName("John")
                        .build())
                .setTotalElements(1)
                .setTotalPages(1)
                .build();
        
        when(userService.searchUsers(request)).thenReturn(expectedResponse);

        // When
        grpcService.searchUsers(request, searchUsersResponseObserver);

        // Then
        verify(userService).searchUsers(request);
        verify(searchUsersResponseObserver).onNext(expectedResponse);
        verify(searchUsersResponseObserver).onCompleted();
    }

    @Test
    @DisplayName("Should handle search users internal error")
    void searchUsers_WithInternalError_ShouldReturnInternalError() {
        // Given
        SearchUsersRequest request = SearchUsersRequest.newBuilder()
                .setQuery("test")
                .build();
        
        when(userService.searchUsers(request))
                .thenThrow(new RuntimeException("Search index unavailable"));

        // When
        grpcService.searchUsers(request, searchUsersResponseObserver);

        // Then
        ArgumentCaptor<StatusException> exceptionCaptor = ArgumentCaptor.forClass(StatusException.class);
        verify(searchUsersResponseObserver).onError(exceptionCaptor.capture());
        
        StatusException exception = exceptionCaptor.getValue();
        assertThat(exception.getStatus().getCode()).isEqualTo(Status.Code.INTERNAL);
        assertThat(exception.getStatus().getDescription()).contains("Internal server error");
    }

    // =============== getUserInfo Tests ===============

    @Test
    @DisplayName("Should get user info successfully")
    void getUserInfo_WithValidRequest_ShouldReturnUserInfo() {
        // Given
        GetUserInfoRequest request = GetUserInfoRequest.newBuilder()
                .setUserId(1L)
                .build();
        
        UserInfoResponse expectedResponse = UserInfoResponse.newBuilder()
                .setUserInfo(UserInfo.newBuilder()
                        .setUserId(1L)
                        .setUsername("testuser")
                        .setEmail("test@example.com")
                        .build())
                .build();
        
        when(userService.getUserInfo(request)).thenReturn(expectedResponse);

        // When
        grpcService.getUserInfo(request, userInfoResponseObserver);

        // Then
        verify(userService).getUserInfo(request);
        verify(userInfoResponseObserver).onNext(expectedResponse);
        verify(userInfoResponseObserver).onCompleted();
    }

    // =============== findUserByEmail Tests ===============

    @Test
    @DisplayName("Should find user by email successfully")
    void findUserByEmail_WithValidEmail_ShouldReturnUser() {
        // Given
        FindUserByEmailRequest request = FindUserByEmailRequest.newBuilder()
                .setEmail("test@example.com")
                .build();
        
        UserInfoResponse expectedResponse = UserInfoResponse.newBuilder()
                .setUserInfo(UserInfo.newBuilder()
                        .setUserId(1L)
                        .setEmail("test@example.com")
                        .build())
                .build();
        
        when(userService.findUserByEmail(request)).thenReturn(expectedResponse);

        // When
        grpcService.findUserByEmail(request, userInfoResponseObserver);

        // Then
        verify(userService).findUserByEmail(request);
        verify(userInfoResponseObserver).onNext(expectedResponse);
        verify(userInfoResponseObserver).onCompleted();
    }

    @Test
    @DisplayName("Should handle user not found by email")
    void findUserByEmail_WithNonExistentEmail_ShouldReturnNotFound() {
        // Given
        FindUserByEmailRequest request = FindUserByEmailRequest.newBuilder()
                .setEmail("nonexistent@example.com")
                .build();
        
        when(userService.findUserByEmail(request))
                .thenThrow(new NotFoundException("User not found with email: nonexistent@example.com"));

        // When
        grpcService.findUserByEmail(request, userInfoResponseObserver);

        // Then
        ArgumentCaptor<StatusException> exceptionCaptor = ArgumentCaptor.forClass(StatusException.class);
        verify(userInfoResponseObserver).onError(exceptionCaptor.capture());
        
        StatusException exception = exceptionCaptor.getValue();
        assertThat(exception.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
        assertThat(exception.getStatus().getDescription()).contains("User not found with email: nonexistent@example.com");
    }

    // =============== findUserByUsername Tests ===============

    @Test
    @DisplayName("Should find user by username successfully")
    void findUserByUsername_WithValidUsername_ShouldReturnUser() {
        // Given
        FindUserByUsernameRequest request = FindUserByUsernameRequest.newBuilder()
                .setUsername("testuser")
                .build();
        
        UserInfoResponse expectedResponse = UserInfoResponse.newBuilder()
                .setUserInfo(UserInfo.newBuilder()
                        .setUserId(1L)
                        .setUsername("testuser")
                        .build())
                .build();
        
        when(userService.findUserByUsername(request)).thenReturn(expectedResponse);

        // When
        grpcService.findUserByUsername(request, userInfoResponseObserver);

        // Then
        verify(userService).findUserByUsername(request);
        verify(userInfoResponseObserver).onNext(expectedResponse);
        verify(userInfoResponseObserver).onCompleted();
    }

    // =============== authenticateUser Tests ===============

    @Test
    @DisplayName("Should authenticate user successfully")
    void authenticateUser_WithValidCredentials_ShouldReturnUser() {
        // Given
        AuthenticateUserRequest request = AuthenticateUserRequest.newBuilder()
                .setUsername("testuser")
                .setPassword("password123")
                .build();
        
        UserInfoResponse expectedResponse = UserInfoResponse.newBuilder()
                .setUserInfo(UserInfo.newBuilder()
                        .setUserId(1L)
                        .setUsername("testuser")
                        .build())
                .build();
        
        when(userService.authenticateUser(request)).thenReturn(expectedResponse);

        // When
        grpcService.authenticateUser(request, userInfoResponseObserver);

        // Then
        verify(userService).authenticateUser(request);
        verify(userInfoResponseObserver).onNext(expectedResponse);
        verify(userInfoResponseObserver).onCompleted();
    }

    @Test
    @DisplayName("Should handle authentication failure with invalid credentials")
    void authenticateUser_WithInvalidCredentials_ShouldReturnUnauthenticated() {
        // Given
        AuthenticateUserRequest request = AuthenticateUserRequest.newBuilder()
                .setUsername("testuser")
                .setPassword("wrongpassword")
                .build();
        
        when(userService.authenticateUser(request))
                .thenThrow(new AuthenticationException("Invalid credentials"));

        // When
        grpcService.authenticateUser(request, userInfoResponseObserver);

        // Then
        ArgumentCaptor<StatusException> exceptionCaptor = ArgumentCaptor.forClass(StatusException.class);
        verify(userInfoResponseObserver).onError(exceptionCaptor.capture());
        
        StatusException exception = exceptionCaptor.getValue();
        assertThat(exception.getStatus().getCode()).isEqualTo(Status.Code.UNAUTHENTICATED);
        assertThat(exception.getStatus().getDescription()).contains("Invalid credentials");
    }

    @Test
    @DisplayName("Should handle authentication failure with user not found")
    void authenticateUser_WithNonExistentUser_ShouldReturnNotFound() {
        // Given
        AuthenticateUserRequest request = AuthenticateUserRequest.newBuilder()
                .setUsername("nonexistentuser")
                .setPassword("password123")
                .build();
        
        when(userService.authenticateUser(request))
                .thenThrow(new NotFoundException("User not found"));

        // When
        grpcService.authenticateUser(request, userInfoResponseObserver);

        // Then
        ArgumentCaptor<StatusException> exceptionCaptor = ArgumentCaptor.forClass(StatusException.class);
        verify(userInfoResponseObserver).onError(exceptionCaptor.capture());
        
        StatusException exception = exceptionCaptor.getValue();
        assertThat(exception.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
        assertThat(exception.getStatus().getDescription()).contains("User not found");
    }

    // =============== updatePassword Tests ===============

    @Test
    @DisplayName("Should update password successfully")
    void updatePassword_WithValidRequest_ShouldReturnSuccess() {
        // Given
        UpdatePasswordRequest request = UpdatePasswordRequest.newBuilder()
                .setUserId(1L)
                .setCurrentPassword("oldpassword")
                .setNewPassword("newpassword")
                .build();
        
        UpdatePasswordResponse expectedResponse = UpdatePasswordResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Password updated successfully")
                .build();
        
        when(userService.updatePassword(request)).thenReturn(expectedResponse);

        // When
        grpcService.updatePassword(request, updatePasswordResponseObserver);

        // Then
        verify(userService).updatePassword(request);
        verify(updatePasswordResponseObserver).onNext(expectedResponse);
        verify(updatePasswordResponseObserver).onCompleted();
    }

    // =============== updateUserLastLogin Tests ===============

    @Test
    @DisplayName("Should update user last login successfully")
    void updateUserLastLogin_WithValidRequest_ShouldReturnSuccess() {
        // Given
        UpdateUserLastLoginRequest request = UpdateUserLastLoginRequest.newBuilder()
                .setUserId(1L)
                .build();
        
        UpdateUserLastLoginResponse expectedResponse = UpdateUserLastLoginResponse.newBuilder()
                .setSuccess(true)
                .build();
        
        when(userService.updateUserLastLogin(request)).thenReturn(expectedResponse);

        // When
        grpcService.updateUserLastLogin(request, updateUserLastLoginResponseObserver);

        // Then
        verify(userService).updateUserLastLogin(request);
        verify(updateUserLastLoginResponseObserver).onNext(expectedResponse);
        verify(updateUserLastLoginResponseObserver).onCompleted();
    }

    // =============== createUser Tests ===============

    @Test
    @DisplayName("Should create user successfully")
    void createUser_WithValidRequest_ShouldReturnCreatedUser() {
        // Given
        CreateUserRequest request = CreateUserRequest.newBuilder()
                .setUsername("newuser")
                .setEmail("newuser@example.com")
                .setPassword("password123")
                .setFirstName("New")
                .setLastName("User")
                .build();
        
        UserProfileResponse expectedResponse = UserProfileResponse.newBuilder()
                .setUserInfo(UserInfo.newBuilder()
                        .setUserId(2L)
                        .setUsername("newuser")
                        .setEmail("newuser@example.com")
                        .build())
                .build();
        
        when(userService.createUser(request)).thenReturn(expectedResponse);

        // When
        grpcService.createUser(request, userProfileResponseObserver);

        // Then
        verify(userService).createUser(request);
        verify(userProfileResponseObserver).onNext(expectedResponse);
        verify(userProfileResponseObserver).onCompleted();
    }

    @Test
    @DisplayName("Should handle invalid argument in createUser")
    void createUser_WithInvalidRequest_ShouldReturnInvalidArgument() {
        // Given
        CreateUserRequest request = CreateUserRequest.newBuilder()
                .setUsername("") // Invalid empty username
                .setEmail("invalid-email")
                .build();
        
        when(userService.createUser(request))
                .thenThrow(new IllegalArgumentException("Username cannot be empty"));

        // When
        grpcService.createUser(request, userProfileResponseObserver);

        // Then
        ArgumentCaptor<StatusException> exceptionCaptor = ArgumentCaptor.forClass(StatusException.class);
        verify(userProfileResponseObserver).onError(exceptionCaptor.capture());
        
        StatusException exception = exceptionCaptor.getValue();
        assertThat(exception.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
        assertThat(exception.getStatus().getDescription()).contains("Username cannot be empty");
    }

    // =============== healthCheck Tests ===============

    @Test
    @DisplayName("Should return healthy status")
    void healthCheck_ShouldReturnHealthyStatus() {
        // Given
        HealthCheckRequest request = HealthCheckRequest.newBuilder().build();

        // When
        grpcService.healthCheck(request, healthCheckResponseObserver);

        // Then
        ArgumentCaptor<HealthCheckResponse> responseCaptor = ArgumentCaptor.forClass(HealthCheckResponse.class);
        verify(healthCheckResponseObserver).onNext(responseCaptor.capture());
        verify(healthCheckResponseObserver).onCompleted();
        
        HealthCheckResponse response = responseCaptor.getValue();
        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("Users service is healthy");
        assertThat(response.getData().getService()).isEqualTo("users-service");
        assertThat(response.getData().getVersion()).isEqualTo("1.0.0");
        assertThat(response.getData().getTimestamp()).isNotEmpty();
        
        // Health check doesn't call userService
        verifyNoInteractions(userService);
    }

    // =============== checkUsernameExists Tests ===============

    @Test
    @DisplayName("Should check username exists successfully")
    void checkUsernameExists_WithExistingUsername_ShouldReturnTrue() {
        // Given
        FindUserByUsernameRequest request = FindUserByUsernameRequest.newBuilder()
                .setUsername("existinguser")
                .build();
        
        when(userService.usernameExists("existinguser")).thenReturn(true);

        // When
        grpcService.checkUsernameExists(request, existsResponseObserver);

        // Then
        ArgumentCaptor<ExistsResponse> responseCaptor = ArgumentCaptor.forClass(ExistsResponse.class);
        verify(existsResponseObserver).onNext(responseCaptor.capture());
        verify(existsResponseObserver).onCompleted();
        
        ExistsResponse response = responseCaptor.getValue();
        assertThat(response.getExists()).isTrue();
        assertThat(response.getMessage()).isEqualTo("Username already exists");
    }

    @Test
    @DisplayName("Should check username exists returns false for non-existing username")
    void checkUsernameExists_WithNonExistingUsername_ShouldReturnFalse() {
        // Given
        FindUserByUsernameRequest request = FindUserByUsernameRequest.newBuilder()
                .setUsername("newuser")
                .build();
        
        when(userService.usernameExists("newuser")).thenReturn(false);

        // When
        grpcService.checkUsernameExists(request, existsResponseObserver);

        // Then
        ArgumentCaptor<ExistsResponse> responseCaptor = ArgumentCaptor.forClass(ExistsResponse.class);
        verify(existsResponseObserver).onNext(responseCaptor.capture());
        verify(existsResponseObserver).onCompleted();
        
        ExistsResponse response = responseCaptor.getValue();
        assertThat(response.getExists()).isFalse();
        assertThat(response.getMessage()).isEqualTo("Username is available");
    }

    @ParameterizedTest
    @ValueSource(strings = {"user1", "test_user", "admin", "guest", "superuser"})
    @DisplayName("Should handle various username formats in existence check")
    void checkUsernameExists_WithVariousUsernames_ShouldHandleCorrectly(String username) {
        // Given
        FindUserByUsernameRequest request = FindUserByUsernameRequest.newBuilder()
                .setUsername(username)
                .build();
        
        when(userService.usernameExists(username)).thenReturn(true);

        // When
        grpcService.checkUsernameExists(request, existsResponseObserver);

        // Then
        verify(userService).usernameExists(username);
        verify(existsResponseObserver).onNext(any(ExistsResponse.class));
        verify(existsResponseObserver).onCompleted();
    }

    // =============== checkEmailExists Tests ===============

    @Test
    @DisplayName("Should check email exists successfully")
    void checkEmailExists_WithExistingEmail_ShouldReturnTrue() {
        // Given
        FindUserByEmailRequest request = FindUserByEmailRequest.newBuilder()
                .setEmail("existing@example.com")
                .build();
        
        when(userService.emailExists("existing@example.com")).thenReturn(true);

        // When
        grpcService.checkEmailExists(request, existsResponseObserver);

        // Then
        ArgumentCaptor<ExistsResponse> responseCaptor = ArgumentCaptor.forClass(ExistsResponse.class);
        verify(existsResponseObserver).onNext(responseCaptor.capture());
        verify(existsResponseObserver).onCompleted();
        
        ExistsResponse response = responseCaptor.getValue();
        assertThat(response.getExists()).isTrue();
        assertThat(response.getMessage()).isEqualTo("Email already exists");
    }

    @Test
    @DisplayName("Should check email exists returns false for non-existing email")
    void checkEmailExists_WithNonExistingEmail_ShouldReturnFalse() {
        // Given
        FindUserByEmailRequest request = FindUserByEmailRequest.newBuilder()
                .setEmail("new@example.com")
                .build();
        
        when(userService.emailExists("new@example.com")).thenReturn(false);

        // When
        grpcService.checkEmailExists(request, existsResponseObserver);

        // Then
        ArgumentCaptor<ExistsResponse> responseCaptor = ArgumentCaptor.forClass(ExistsResponse.class);
        verify(existsResponseObserver).onNext(responseCaptor.capture());
        verify(existsResponseObserver).onCompleted();
        
        ExistsResponse response = responseCaptor.getValue();
        assertThat(response.getExists()).isFalse();
        assertThat(response.getMessage()).isEqualTo("Email is available");
    }

    // =============== deleteUser Tests ===============

    @Test
    @DisplayName("Should delete user successfully")
    void deleteUser_WithValidRequest_ShouldReturnSuccess() {
        // Given
        DeleteUserRequest request = DeleteUserRequest.newBuilder()
                .setUserId(1L)
                .build();
        
        when(userService.deleteUser(1L)).thenReturn(true);

        // When
        grpcService.deleteUser(request, deleteUserResponseObserver);

        // Then
        ArgumentCaptor<DeleteUserResponse> responseCaptor = ArgumentCaptor.forClass(DeleteUserResponse.class);
        verify(deleteUserResponseObserver).onNext(responseCaptor.capture());
        verify(deleteUserResponseObserver).onCompleted();
        
        DeleteUserResponse response = responseCaptor.getValue();
        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("User deleted successfully");
    }

    @Test
    @DisplayName("Should handle delete user failure")
    void deleteUser_WithFailure_ShouldReturnFailure() {
        // Given
        DeleteUserRequest request = DeleteUserRequest.newBuilder()
                .setUserId(999L)
                .build();
        
        when(userService.deleteUser(999L)).thenReturn(false);

        // When
        grpcService.deleteUser(request, deleteUserResponseObserver);

        // Then
        ArgumentCaptor<DeleteUserResponse> responseCaptor = ArgumentCaptor.forClass(DeleteUserResponse.class);
        verify(deleteUserResponseObserver).onNext(responseCaptor.capture());
        verify(deleteUserResponseObserver).onCompleted();
        
        DeleteUserResponse response = responseCaptor.getValue();
        assertThat(response.getSuccess()).isFalse();
        assertThat(response.getMessage()).isEqualTo("Failed to delete user");
    }

    @Test
    @DisplayName("Should handle delete user internal error")
    void deleteUser_WithInternalError_ShouldReturnInternalError() {
        // Given
        DeleteUserRequest request = DeleteUserRequest.newBuilder()
                .setUserId(1L)
                .build();
        
        when(userService.deleteUser(1L))
                .thenThrow(new RuntimeException("Database connection failed"));

        // When
        grpcService.deleteUser(request, deleteUserResponseObserver);

        // Then
        ArgumentCaptor<StatusException> exceptionCaptor = ArgumentCaptor.forClass(StatusException.class);
        verify(deleteUserResponseObserver).onError(exceptionCaptor.capture());
        
        StatusException exception = exceptionCaptor.getValue();
        assertThat(exception.getStatus().getCode()).isEqualTo(Status.Code.INTERNAL);
        assertThat(exception.getStatus().getDescription()).contains("Internal server error");
    }

    // =============== Generic Error Handling Tests ===============

    @Test
    @DisplayName("Should handle unexpected exceptions properly")
    void grpcMethods_WithUnexpectedExceptions_ShouldReturnInternalError() {
        // Given
        GetUserProfileRequest request = GetUserProfileRequest.newBuilder()
                .setUserId(1L)
                .build();
        
        when(userService.getUserProfile(request))
                .thenThrow(new NullPointerException("Unexpected null pointer"));

        // When
        grpcService.getUserProfile(request, userProfileResponseObserver);

        // Then
        ArgumentCaptor<StatusException> exceptionCaptor = ArgumentCaptor.forClass(StatusException.class);
        verify(userProfileResponseObserver).onError(exceptionCaptor.capture());
        
        StatusException exception = exceptionCaptor.getValue();
        assertThat(exception.getStatus().getCode()).isEqualTo(Status.Code.INTERNAL);
        assertThat(exception.getStatus().getDescription()).contains("Internal server error");
        assertThat(exception.getStatus().getDescription()).contains("Unexpected null pointer");
    }

    @Test
    @DisplayName("Should handle all error types with appropriate gRPC status codes")
    void grpcErrorHandling_ShouldMapExceptionsCorrectly() {
        // This test verifies our error mapping is consistent across all methods
        
        // NOT_FOUND -> Status.NOT_FOUND
        GetUserProfileRequest profileRequest = GetUserProfileRequest.newBuilder().setUserId(1L).build();
        when(userService.getUserProfile(profileRequest)).thenThrow(new NotFoundException("User not found"));
        grpcService.getUserProfile(profileRequest, userProfileResponseObserver);
        
        ArgumentCaptor<StatusException> notFoundCaptor = ArgumentCaptor.forClass(StatusException.class);
        verify(userProfileResponseObserver).onError(notFoundCaptor.capture());
        assertThat(notFoundCaptor.getValue().getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
        
        // Reset mocks for next test
        reset(userProfileResponseObserver, userService);
        
        // UNAUTHENTICATED -> Status.UNAUTHENTICATED
        AuthenticateUserRequest authRequest = AuthenticateUserRequest.newBuilder()
                .setUsername("test").setPassword("test").build();
        when(userService.authenticateUser(authRequest)).thenThrow(new AuthenticationException("Invalid credentials"));
        grpcService.authenticateUser(authRequest, userInfoResponseObserver);
        
        ArgumentCaptor<StatusException> unauthCaptor = ArgumentCaptor.forClass(StatusException.class);
        verify(userInfoResponseObserver).onError(unauthCaptor.capture());
        assertThat(unauthCaptor.getValue().getStatus().getCode()).isEqualTo(Status.Code.UNAUTHENTICATED);
        
        // Reset mocks for next test
        reset(userInfoResponseObserver, userService);
        
        // INVALID_ARGUMENT -> Status.INVALID_ARGUMENT
        UpdateUserProfileRequest updateRequest = UpdateUserProfileRequest.newBuilder().setUserId(1L).build();
        when(userService.updateUserProfile(updateRequest)).thenThrow(new IllegalArgumentException("Invalid input"));
        grpcService.updateUserProfile(updateRequest, userProfileResponseObserver);
        
        ArgumentCaptor<StatusException> invalidArgCaptor = ArgumentCaptor.forClass(StatusException.class);
        verify(userProfileResponseObserver).onError(invalidArgCaptor.capture());
        assertThat(invalidArgCaptor.getValue().getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
    }

    @Test
    void placeholder() {
        assertThat(true).isTrue();
    }
} 