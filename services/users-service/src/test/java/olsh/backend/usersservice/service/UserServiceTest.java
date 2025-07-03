package olsh.backend.usersservice.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.olsh.users.proto.AuthenticateUserRequest;
import com.olsh.users.proto.CreateUserRequest;
import com.olsh.users.proto.FindUserByEmailRequest;
import com.olsh.users.proto.FindUserByUsernameRequest;
import com.olsh.users.proto.GetUserInfoRequest;
import com.olsh.users.proto.GetUserProfileRequest;
import com.olsh.users.proto.SearchUsersRequest;
import com.olsh.users.proto.SearchUsersResponse;
import com.olsh.users.proto.UpdatePasswordRequest;
import com.olsh.users.proto.UpdatePasswordResponse;
import com.olsh.users.proto.UpdateUserLastLoginRequest;
import com.olsh.users.proto.UpdateUserLastLoginResponse;
import com.olsh.users.proto.UpdateUserProfileRequest;
import com.olsh.users.proto.UserInfoResponse;
import com.olsh.users.proto.UserProfileResponse;

import olsh.backend.usersservice.config.PointsConfig;
import olsh.backend.usersservice.entity.Role;
import olsh.backend.usersservice.entity.User;
import olsh.backend.usersservice.exception.AuthenticationException;
import olsh.backend.usersservice.exception.NotFoundException;
import olsh.backend.usersservice.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PointsConfig pointsConfig;

    @Mock
    private UserStatsService userStatsService;

    @InjectMocks
    private UserService userService;

    private User testUser;


    @BeforeEach
    void setUp() {
        // Minimal test user setup
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .firstName("John")
                .lastName("Doe")
                .role(Role.ROLE_USER)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // === Core CRUD Operations Tests ===

    @Test
    @DisplayName("Should find user by ID successfully")
    void findById_WithExistingId_ShouldReturnUser() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        User result = userService.findById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUsername()).isEqualTo("testuser");
        verify(userRepository).findById(1L);
    }

    @Test
    @DisplayName("Should throw NotFoundException when user not found by ID")
    void findById_WithNonExistentId_ShouldThrowNotFoundException() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.findById(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("User not found with ID: 999");
        
        verify(userRepository).findById(999L);
    }

    @Test
    @DisplayName("Should create user successfully")
    void createUser_WithValidData_ShouldCreateUser() {
        // Given
        CreateUserRequest request = CreateUserRequest.newBuilder()
                .setUsername("newuser")
                .setEmail("newuser@example.com")
                .setPassword("plainPassword")
                .setFirstName("Jane")
                .setLastName("Smith")
                .setRole("ROLE_USER")
                .build();

        User newUser = User.builder()
                .id(2L)
                .username("newuser")
                .email("newuser@example.com")
                .password("encodedPassword")
                .firstName("Jane")
                .lastName("Smith")
                .role(Role.ROLE_USER)
                .build();

        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("newuser@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("plainPassword")).thenReturn("encodedPassword");
        when(pointsConfig.getInitialBalance()).thenReturn(10);
        when(userRepository.save(any(User.class))).thenReturn(newUser);

        // When
        UserProfileResponse result = userService.createUser(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserInfo().getUsername()).isEqualTo("newuser");
        assertThat(result.getUserInfo().getEmail()).isEqualTo("newuser@example.com");
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        
        verify(userRepository).findByUsername("newuser");
        verify(userRepository).findByEmail("newuser@example.com");
        verify(passwordEncoder).encode("plainPassword");
        verify(pointsConfig, atLeastOnce()).getInitialBalance();
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when username already exists")
    void createUser_WithExistingUsername_ShouldThrowIllegalArgumentException() {
        // Given
        CreateUserRequest request = CreateUserRequest.newBuilder()
                .setUsername("testuser")
                .setEmail("new@example.com")
                .setPassword("password")
                .setFirstName("John")
                .setLastName("Doe")
                .setRole("ROLE_USER")
                .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When & Then
        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User with this username already exists");
        
        verify(userRepository).findByUsername("testuser");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when email already exists")
    void createUser_WithExistingEmail_ShouldThrowIllegalArgumentException() {
        // Given
        CreateUserRequest request = CreateUserRequest.newBuilder()
                .setUsername("newuser")
                .setEmail("test@example.com")
                .setPassword("password")
                .setFirstName("John")
                .setLastName("Doe")
                .setRole("ROLE_USER")
                .build();

        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // When & Then
        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User with this email already exists");
        
        verify(userRepository).findByUsername("newuser");
        verify(userRepository).findByEmail("test@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should delete user successfully")
    void deleteUser_WithExistingId_ShouldReturnTrue() {
        // Given
        when(userRepository.existsById(1L)).thenReturn(true);
        doNothing().when(userRepository).deleteById(1L);

        // When
        boolean result = userService.deleteUser(1L);

        // Then
        assertThat(result).isTrue();
        verify(userRepository).existsById(1L);
        verify(userRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Should return false when deleting non-existent user")
    void deleteUser_WithNonExistentId_ShouldReturnFalse() {
        // Given
        when(userRepository.existsById(999L)).thenReturn(false);

        // When
        boolean result = userService.deleteUser(999L);

        // Then
        assertThat(result).isFalse();
        verify(userRepository).existsById(999L);
        verify(userRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Should return false when delete operation throws exception")
    void deleteUser_WithException_ShouldReturnFalse() {
        // Given
        when(userRepository.existsById(1L)).thenReturn(true);
        doThrow(new RuntimeException("Database error")).when(userRepository).deleteById(1L);

        // When
        boolean result = userService.deleteUser(1L);

        // Then
        assertThat(result).isFalse();
        verify(userRepository).existsById(1L);
        verify(userRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Should check if username exists")
    void usernameExists_WithExistingUsername_ShouldReturnTrue() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When
        boolean result = userService.usernameExists("testuser");

        // Then
        assertThat(result).isTrue();
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    @DisplayName("Should check if username does not exist")
    void usernameExists_WithNonExistentUsername_ShouldReturnFalse() {
        // Given
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When
        boolean result = userService.usernameExists("nonexistent");

        // Then
        assertThat(result).isFalse();
        verify(userRepository).findByUsername("nonexistent");
    }

    @Test
    @DisplayName("Should check if email exists")
    void emailExists_WithExistingEmail_ShouldReturnTrue() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // When
        boolean result = userService.emailExists("test@example.com");

        // Then
        assertThat(result).isTrue();
        verify(userRepository).findByEmail("test@example.com");
    }

    @Test
    @DisplayName("Should check if email does not exist")
    void emailExists_WithNonExistentEmail_ShouldReturnFalse() {
        // Given
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // When
        boolean result = userService.emailExists("nonexistent@example.com");

        // Then
        assertThat(result).isFalse();
        verify(userRepository).findByEmail("nonexistent@example.com");
    }

    // === Profile Management Tests ===

    @Test
    @DisplayName("Should get user profile successfully")
    void getUserProfile_WithValidId_ShouldReturnProfile() {
        // Given
        GetUserProfileRequest request = GetUserProfileRequest.newBuilder()
                .setUserId(1L)
                .build();
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        UserProfileResponse result = userService.getUserProfile(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserInfo().getUserId()).isEqualTo(1L);
        assertThat(result.getUserInfo().getUsername()).isEqualTo("testuser");
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        verify(userRepository).findById(1L);
    }

    @Test
    @DisplayName("Should update user profile with first name only")
    void updateUserProfile_WithFirstNameOnly_ShouldUpdateFirstName() {
        // Given
        UpdateUserProfileRequest request = UpdateUserProfileRequest.newBuilder()
                .setUserId(1L)
                .setFirstName("Jane")
                .setLastName("")
                .setEmail("")
                .setUsername("")
                .setPassword("")
                .build();

        User updatedUser = User.builder()
                .id(testUser.getId())
                .username(testUser.getUsername())
                .email(testUser.getEmail())
                .password(testUser.getPassword())
                .firstName("Jane")
                .lastName(testUser.getLastName())
                .role(testUser.getRole())
                .createdAt(testUser.getCreatedAt())
                .lastLoginAt(testUser.getLastLoginAt())
                .build();
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        // When
        UserProfileResponse result = userService.updateUserProfile(request);

        // Then
        assertThat(result.getUserInfo().getFirstName()).isEqualTo("Jane");
        verify(userRepository).findById(1L);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should update user profile with email validation")
    void updateUserProfile_WithNewEmail_ShouldUpdateEmail() {
        // Given
        UpdateUserProfileRequest request = UpdateUserProfileRequest.newBuilder()
                .setUserId(1L)
                .setFirstName("")
                .setLastName("")
                .setEmail("newemail@example.com")
                .setUsername("")
                .setPassword("")
                .build();

        User updatedUser = User.builder()
                .id(testUser.getId())
                .username(testUser.getUsername())
                .email("newemail@example.com")
                .password(testUser.getPassword())
                .firstName(testUser.getFirstName())
                .lastName(testUser.getLastName())
                .role(testUser.getRole())
                .createdAt(testUser.getCreatedAt())
                .lastLoginAt(testUser.getLastLoginAt())
                .build();
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findByEmail("newemail@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        // When
        UserProfileResponse result = userService.updateUserProfile(request);

        // Then
        assertThat(result.getUserInfo().getEmail()).isEqualTo("newemail@example.com");
        verify(userRepository).findById(1L);
        verify(userRepository).findByEmail("newemail@example.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when updating to existing email")
    void updateUserProfile_WithExistingEmail_ShouldThrowException() {
        // Given
        UpdateUserProfileRequest request = UpdateUserProfileRequest.newBuilder()
                .setUserId(1L)
                .setEmail("existing@example.com")
                .build();

        User otherUser = User.builder().id(2L).email("existing@example.com").build();
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(otherUser));

        // When & Then
        assertThatThrownBy(() -> userService.updateUserProfile(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email already in use by another user");
        
        verify(userRepository).findById(1L);
        verify(userRepository).findByEmail("existing@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should update username with validation")
    void updateUserProfile_WithNewUsername_ShouldUpdateUsername() {
        // Given
        UpdateUserProfileRequest request = UpdateUserProfileRequest.newBuilder()
                .setUserId(1L)
                .setUsername("newusername")
                .build();

        User updatedUser = User.builder()
                .id(testUser.getId())
                .username("newusername")
                .email(testUser.getEmail())
                .password(testUser.getPassword())
                .firstName(testUser.getFirstName())
                .lastName(testUser.getLastName())
                .role(testUser.getRole())
                .createdAt(testUser.getCreatedAt())
                .lastLoginAt(testUser.getLastLoginAt())
                .build();
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findByUsername("newusername")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        // When
        UserProfileResponse result = userService.updateUserProfile(request);

        // Then
        assertThat(result.getUserInfo().getUsername()).isEqualTo("newusername");
        verify(userRepository).findById(1L);
        verify(userRepository).findByUsername("newusername");
        verify(userRepository).save(any(User.class)); // Service saves once after all updates
    }

    @Test
    @DisplayName("Should update password with encoding")
    void updateUserProfile_WithPassword_ShouldEncodeAndUpdatePassword() {
        // Given
        UpdateUserProfileRequest request = UpdateUserProfileRequest.newBuilder()
                .setUserId(1L)
                .setPassword("newPlainPassword")
                .build();

        User updatedUser = User.builder()
                .id(testUser.getId())
                .username(testUser.getUsername())
                .email(testUser.getEmail())
                .password("newEncodedPassword")
                .firstName(testUser.getFirstName())
                .lastName(testUser.getLastName())
                .role(testUser.getRole())
                .createdAt(testUser.getCreatedAt())
                .lastLoginAt(testUser.getLastLoginAt())
                .build();
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);
        when(passwordEncoder.encode("newPlainPassword")).thenReturn("newEncodedPassword");

        // When
        UserProfileResponse result = userService.updateUserProfile(request);

        // Then
        assertThat(result).isNotNull();
        verify(userRepository).findById(1L);
        verify(passwordEncoder).encode("newPlainPassword");
        verify(userRepository, times(2)).save(any(User.class));
    }

    @Test
    @DisplayName("Should not update fields with empty values")
    void updateUserProfile_WithEmptyValues_ShouldNotUpdate() {
        // Given - only test empty string, as service uses isEmpty() not isBlank()
        UpdateUserProfileRequest request = UpdateUserProfileRequest.newBuilder()
                .setUserId(1L)
                .setFirstName("")
                .setLastName("")
                .setEmail("")
                .setPassword("")
                .build();
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserProfileResponse result = userService.updateUserProfile(request);

        // Then
        assertThat(result.getUserInfo().getFirstName()).isEqualTo("John"); // Original value
        assertThat(result.getUserInfo().getLastName()).isEqualTo("Doe"); // Original value
        verify(userRepository).findById(1L);
        verify(userRepository).save(any(User.class));
        verify(passwordEncoder, never()).encode(anyString());
    }

    @ParameterizedTest
    @ValueSource(strings = {" ", "\t", "\n"})
    @DisplayName("Should update fields with whitespace values (service behavior)")
    void updateUserProfile_WithWhitespaceValues_ShouldUpdate(String whitespaceValue) {
        // Given - service uses isEmpty() so whitespace values will be updated
        UpdateUserProfileRequest request = UpdateUserProfileRequest.newBuilder()
                .setUserId(1L)
                .setFirstName(whitespaceValue)
                .setLastName(whitespaceValue)
                .setEmail("")
                .setPassword(whitespaceValue)
                .build();
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(passwordEncoder.encode(whitespaceValue)).thenReturn("encodedWhitespace");

        // When
        UserProfileResponse result = userService.updateUserProfile(request);

        // Then - values should be updated to whitespace since service uses isEmpty()
        verify(userRepository).findById(1L);
        verify(userRepository, times(2)).save(any(User.class)); // Once for profile, once for password
        verify(passwordEncoder).encode(whitespaceValue);
    }

    // === Authentication & Security Tests ===

    @Test
    @DisplayName("Should authenticate user with username successfully")
    void authenticateUser_WithValidUsernameCredentials_ShouldReturnUserInfo() {
        // Given
        AuthenticateUserRequest request = AuthenticateUserRequest.newBuilder()
                .setUsername("testuser")
                .setPassword("plainPassword")
                .setUsingEmail(false)
                .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("plainPassword", "encodedPassword")).thenReturn(true);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserInfoResponse result = userService.authenticateUser(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserInfo().getUsername()).isEqualTo("testuser");
        verify(userRepository).findByUsername("testuser");
        verify(passwordEncoder).matches("plainPassword", "encodedPassword");
        verify(userRepository).save(any(User.class)); // For last login update
    }

    @Test
    @DisplayName("Should authenticate user with email successfully")
    void authenticateUser_WithValidEmailCredentials_ShouldReturnUserInfo() {
        // Given
        AuthenticateUserRequest request = AuthenticateUserRequest.newBuilder()
                .setUsername("test@example.com")
                .setPassword("plainPassword")
                .setUsingEmail(true)
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("plainPassword", "encodedPassword")).thenReturn(true);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserInfoResponse result = userService.authenticateUser(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserInfo().getEmail()).isEqualTo("test@example.com");
        verify(userRepository).findByEmail("test@example.com");
        verify(passwordEncoder).matches("plainPassword", "encodedPassword");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw NotFoundException when user not found during authentication")
    void authenticateUser_WithNonExistentUser_ShouldThrowNotFoundException() {
        // Given
        AuthenticateUserRequest request = AuthenticateUserRequest.newBuilder()
                .setUsername("nonexistent")
                .setPassword("password")
                .setUsingEmail(false)
                .build();

        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.authenticateUser(request))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("User not found");
        
        verify(userRepository).findByUsername("nonexistent");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("Should throw AuthenticationException when password is incorrect")
    void authenticateUser_WithIncorrectPassword_ShouldThrowAuthenticationException() {
        // Given
        AuthenticateUserRequest request = AuthenticateUserRequest.newBuilder()
                .setUsername("testuser")
                .setPassword("wrongPassword")
                .setUsingEmail(false)
                .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> userService.authenticateUser(request))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Invalid credentials");
        
        verify(userRepository).findByUsername("testuser");
        verify(passwordEncoder).matches("wrongPassword", "encodedPassword");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should update password with current password verification")
    void updatePassword_WithValidCurrentPassword_ShouldUpdatePassword() {
        // Given
        UpdatePasswordRequest request = UpdatePasswordRequest.newBuilder()
                .setUserId(1L)
                .setCurrentPassword("currentPlainPassword")
                .setNewPassword("newPlainPassword")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("currentPlainPassword", "encodedPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPlainPassword")).thenReturn("newEncodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UpdatePasswordResponse result = userService.updatePassword(request);

        // Then
        assertThat(result.getSuccess()).isTrue();
        assertThat(result.getMessage()).isEqualTo("Password updated successfully");
        verify(userRepository).findById(1L);
        verify(passwordEncoder).matches("currentPlainPassword", "encodedPassword");
        verify(passwordEncoder).encode("newPlainPassword");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should reject password update with incorrect current password")
    void updatePassword_WithIncorrectCurrentPassword_ShouldReturnFailure() {
        // Given
        UpdatePasswordRequest request = UpdatePasswordRequest.newBuilder()
                .setUserId(1L)
                .setCurrentPassword("wrongCurrentPassword")
                .setNewPassword("newPlainPassword")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongCurrentPassword", "encodedPassword")).thenReturn(false);

        // When
        UpdatePasswordResponse result = userService.updatePassword(request);

        // Then
        assertThat(result.getSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("Current password is incorrect");
        verify(userRepository).findById(1L);
        verify(passwordEncoder).matches("wrongCurrentPassword", "encodedPassword");
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should update password without current password verification")
    void updatePassword_WithoutCurrentPassword_ShouldUpdatePassword() {
        // Given
        UpdatePasswordRequest request = UpdatePasswordRequest.newBuilder()
                .setUserId(1L)
                .setCurrentPassword("") // Empty current password
                .setNewPassword("newPlainPassword")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("newPlainPassword")).thenReturn("newEncodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UpdatePasswordResponse result = userService.updatePassword(request);

        // Then
        assertThat(result.getSuccess()).isTrue();
        assertThat(result.getMessage()).isEqualTo("Password updated successfully");
        verify(userRepository).findById(1L);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(passwordEncoder).encode("newPlainPassword");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should update user last login time")
    void updateUserLastLogin_WithValidUserId_ShouldUpdateLastLoginTime() {
        // Given
        UpdateUserLastLoginRequest request = UpdateUserLastLoginRequest.newBuilder()
                .setUserId(1L)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UpdateUserLastLoginResponse result = userService.updateUserLastLogin(request);

        // Then
        assertThat(result.getSuccess()).isTrue();
        verify(userRepository).findById(1L);
        verify(userRepository).save(any(User.class));
    }

    // === Search & Info Tests ===

    @Test
    @DisplayName("Should search users with pagination")
    void searchUsers_WithValidQuery_ShouldReturnPagedResults() {
        // Given
        SearchUsersRequest request = SearchUsersRequest.newBuilder()
                .setQuery("john")
                .setPage(1)
                .setSize(10)
                .build();

        List<User> users = List.of(testUser);
        Page<User> userPage = new PageImpl<>(users, PageRequest.of(0, 10), 1);
        
        when(userRepository.findByUsernameOrNameContainingIgnoreCase(eq("john"), any(PageRequest.class)))
                .thenReturn(userPage);

        // When
        SearchUsersResponse result = userService.searchUsers(request);

        // Then
        assertThat(result.getUsersCount()).isEqualTo(1);
        assertThat(result.getTotalPages()).isEqualTo(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getUsers(0).getUsername()).isEqualTo("testuser");
        verify(userRepository).findByUsernameOrNameContainingIgnoreCase(eq("john"), any(PageRequest.class));
    }

    @ParameterizedTest
    @CsvSource({
        "0, 10, 0, 10",
        "-1, 5, 0, 5",
        "2, 0, 1, 10",
        "3, -5, 2, 10"
    })
    @DisplayName("Should handle invalid pagination parameters")
    void searchUsers_WithInvalidPagination_ShouldUseDefaults(int inputPage, int inputSize, int expectedPage, int expectedSize) {
        // Given
        SearchUsersRequest request = SearchUsersRequest.newBuilder()
                .setQuery("test")
                .setPage(inputPage)
                .setSize(inputSize)
                .build();

        Page<User> emptyPage = new PageImpl<>(List.of(), PageRequest.of(expectedPage, expectedSize), 0);
        when(userRepository.findByUsernameOrNameContainingIgnoreCase(eq("test"), any(PageRequest.class)))
                .thenReturn(emptyPage);

        // When
        SearchUsersResponse result = userService.searchUsers(request);

        // Then
        assertThat(result).isNotNull();
        verify(userRepository).findByUsernameOrNameContainingIgnoreCase(eq("test"), eq(PageRequest.of(expectedPage, expectedSize)));
    }

    @Test
    @DisplayName("Should get user info by ID")
    void getUserInfo_WithValidId_ShouldReturnUserInfo() {
        // Given
        GetUserInfoRequest request = GetUserInfoRequest.newBuilder()
                .setUserId(1L)
                .build();
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        UserInfoResponse result = userService.getUserInfo(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserInfo().getUserId()).isEqualTo(1L);
        assertThat(result.getUserInfo().getUsername()).isEqualTo("testuser");
        verify(userRepository).findById(1L);
    }

    @Test
    @DisplayName("Should find user by email")
    void findUserByEmail_WithExistingEmail_ShouldReturnUserInfo() {
        // Given
        FindUserByEmailRequest request = FindUserByEmailRequest.newBuilder()
                .setEmail("test@example.com")
                .build();
        
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // When
        UserInfoResponse result = userService.findUserByEmail(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserInfo().getEmail()).isEqualTo("test@example.com");
        verify(userRepository).findByEmail("test@example.com");
    }

    @Test
    @DisplayName("Should throw NotFoundException when email not found")
    void findUserByEmail_WithNonExistentEmail_ShouldThrowNotFoundException() {
        // Given
        FindUserByEmailRequest request = FindUserByEmailRequest.newBuilder()
                .setEmail("nonexistent@example.com")
                .build();
        
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.findUserByEmail(request))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("User not found with email: nonexistent@example.com");
        
        verify(userRepository).findByEmail("nonexistent@example.com");
    }

    @Test
    @DisplayName("Should find user by username")
    void findUserByUsername_WithExistingUsername_ShouldReturnUserInfo() {
        // Given
        FindUserByUsernameRequest request = FindUserByUsernameRequest.newBuilder()
                .setUsername("testuser")
                .build();
        
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When
        UserInfoResponse result = userService.findUserByUsername(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserInfo().getUsername()).isEqualTo("testuser");
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    @DisplayName("Should throw NotFoundException when username not found")
    void findUserByUsername_WithNonExistentUsername_ShouldThrowNotFoundException() {
        // Given
        FindUserByUsernameRequest request = FindUserByUsernameRequest.newBuilder()
                .setUsername("nonexistent")
                .build();
        
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.findUserByUsername(request))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("User not found with username: nonexistent");
        
        verify(userRepository).findByUsername("nonexistent");
    }
}