package olsh.backend.usersservice.service;

import java.time.LocalDateTime;
import java.util.Optional;

import com.olsh.users.proto.AuthenticateUserRequest;
import com.olsh.users.proto.CreateUserRequest;
import com.olsh.users.proto.FindUserByEmailRequest;
import com.olsh.users.proto.FindUserByUsernameRequest;
import com.olsh.users.proto.GetUserInfoRequest;
import com.olsh.users.proto.GetUserProfileRequest;
import com.olsh.users.proto.GetUsersCountRequest;
import com.olsh.users.proto.GetUsersCountResponse;
import com.olsh.users.proto.IncrementLabsReviewedRequest;
import com.olsh.users.proto.IncrementLabsSolvedRequest;
import com.olsh.users.proto.OperationResponse;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import olsh.backend.usersservice.config.PointsConfig;
import olsh.backend.usersservice.entity.Role;
import olsh.backend.usersservice.entity.User;
import olsh.backend.usersservice.exception.AuthenticationException;
import olsh.backend.usersservice.exception.InsufficientBalanceException;
import olsh.backend.usersservice.exception.NotFoundException;
import olsh.backend.usersservice.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PointsConfig pointsConfig;
    private final UserStatsService userStatsService;

    public User findById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("User not found with ID: " + id));
    }

    public UserProfileResponse getUserProfile(GetUserProfileRequest request) {
        User user = findById(request.getUserId());
        log.info("Fetching profile for user with ID: {}", request.getUserId());
        return buildUserProfileResponse(user);
    }

    public UserProfileResponse updateUserProfile(UpdateUserProfileRequest request) {
        User user = findById(request.getUserId());

        if (!request.getFirstName().isEmpty()) {
            user.setFirstName(request.getFirstName());
        }

        if (!request.getLastName().isEmpty()) {
            user.setLastName(request.getLastName());
        }

        if (!request.getEmail().isEmpty()) {
            Optional<User> existingUserWithEmail = userRepository.findByEmail(request.getEmail());
            if (existingUserWithEmail.isPresent() && !existingUserWithEmail.get().getId()
                .equals(user.getId())) {
                throw new IllegalArgumentException("Email already in use by another user");
            }
            user.setEmail(request.getEmail());
        }

        // Handle username update if provided
        String username = request.getUsername();
        boolean usernameChanged = false;
        if (!username.isBlank() && !username.equals(user.getUsername())) {
            Optional<User> existingUserWithUsername = userRepository.findByUsername(username);
            if (existingUserWithUsername.isPresent() && !existingUserWithUsername.get().getId()
                .equals(user.getId())) {
                throw new IllegalArgumentException("Username already in use by another user");
            }

            // Only consider the username changed if it's different from the current one
            if (!username.equals(user.getUsername())) {
                user.setUsername(username);
                usernameChanged = true;
                log.info("Username updated for user ID: {}", request.getUserId());
            }
        }

        User updatedUser = userRepository.save(user);

        if (usernameChanged) {
            // This keeps the usernameChanged variable used
            log.info("Username changed flag is set to: {}", usernameChanged);
        }

        // The password handling is now done directly here, not via auth-service
        String password = request.getPassword();
        if (!password.isEmpty()) {
            try {
                // Encode the password and update it
                String encodedPassword = passwordEncoder.encode(password);
                updatedUser.setPassword(encodedPassword);
                updatedUser = userRepository.save(updatedUser);
                log.info("Updated password for user ID: {}", request.getUserId());
            } catch (Exception e) {
                log.error("Failed to update password for user ID: {}", request.getUserId(), e);
                throw new RuntimeException("Failed to update password: " + e.getMessage(), e);
            }
        }

        log.info("Updated profile for user with ID: {}", request.getUserId());
        return buildUserProfileResponse(updatedUser);
    }

    public SearchUsersResponse searchUsers(SearchUsersRequest request) {
        int page = Math.max(0, request.getPage() - 1); // Convert 1-based page to 0-based
        int size = request.getSize() > 0 ? request.getSize() : 10;

        Page<User> userPage = userRepository.findByUsernameOrNameContainingIgnoreCase(
            request.getQuery(), PageRequest.of(page, size));

        SearchUsersResponse.Builder responseBuilder = SearchUsersResponse.newBuilder();

        userPage.getContent().forEach(user -> responseBuilder.addUsers(buildUserInfo(user)));

        responseBuilder.setTotalPages(userPage.getTotalPages());
        responseBuilder.setTotalElements(userPage.getTotalElements());

        return responseBuilder.build();
    }

    public UserInfoResponse getUserInfo(GetUserInfoRequest request) {
        User user = findById(request.getUserId());
        return UserInfoResponse.newBuilder()
            .setUserInfo(buildUserInfo(user))
            .build();
    }

    public UserInfoResponse findUserByEmail(FindUserByEmailRequest request) {
        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());

        if (userOpt.isEmpty()) {
            throw new NotFoundException("User not found with email: " + request.getEmail());
        }

        User user = userOpt.get();
        return UserInfoResponse.newBuilder()
            .setUserInfo(buildUserInfo(user))
            .build();
    }

    public UserInfoResponse findUserByUsername(FindUserByUsernameRequest request) {
        Optional<User> userOpt = userRepository.findByUsername(request.getUsername());

        if (userOpt.isEmpty()) {
            throw new NotFoundException("User not found with username: " + request.getUsername());
        }

        User user = userOpt.get();
        return UserInfoResponse.newBuilder()
            .setUserInfo(buildUserInfo(user))
            .build();
    }

    public UserProfileResponse createUser(CreateUserRequest request) {
        Optional<User> existingUserWithUsername =
            userRepository.findByUsername(request.getUsername());
        if (existingUserWithUsername.isPresent()) {
            throw new IllegalArgumentException("User with this username already exists");
        }

        Optional<User> existingUserWithEmail = userRepository.findByEmail(request.getEmail());
        if (existingUserWithEmail.isPresent()) {
            throw new IllegalArgumentException("User with this email already exists");
        }        
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        User user = User.builder()
            .username(request.getUsername())
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .email(request.getEmail())
            .role(Role.valueOf(request.getRole()))
            .password(encodedPassword)
            .labsSolved(0)
            .labsReviewed(0)
            .balance(pointsConfig.getInitialBalance())
            .build();

        User createdUser = userRepository.save(user);
        log.info("Created user profile for username: {} with initial balance: {}", 
                 request.getUsername(), pointsConfig.getInitialBalance());

        return buildUserProfileResponse(createdUser);
    }

    public UserInfoResponse authenticateUser(AuthenticateUserRequest request) {
        Optional<User> userOpt;
        if (request.getUsingEmail()) {
            userOpt = userRepository.findByEmail(request.getUsername());
        } else {
            userOpt = userRepository.findByUsername(request.getUsername());
        }

        if (userOpt.isEmpty()) {
            throw new NotFoundException("User not found");
        }

        User user = userOpt.get();

        // Check password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AuthenticationException("Invalid credentials");
        }

        // Update last login time
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        return UserInfoResponse.newBuilder()
            .setUserInfo(buildUserInfo(user))
            .build();
    }

    public UpdatePasswordResponse updatePassword(UpdatePasswordRequest request) {
        User user = findById(request.getUserId());

        // If current password is provided, verify it (for normal password change)
        if (!request.getCurrentPassword().isEmpty()) {
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                return UpdatePasswordResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Current password is incorrect")
                    .build();
            }
        }

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return UpdatePasswordResponse.newBuilder()
            .setSuccess(true)
            .setMessage("Password updated successfully")
            .build();
    }

    public UpdateUserLastLoginResponse updateUserLastLogin(UpdateUserLastLoginRequest request) {
        User user = findById(request.getUserId());
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        return UpdateUserLastLoginResponse.newBuilder()
            .setSuccess(true)
            .build();
    }

    /**
     * Check if a username already exists in the system
     *
     * @param username The username to check
     * @return true if the username exists, false otherwise
     */
    public boolean usernameExists(String username) {
        log.debug("Checking if username exists: {}", username);
        return userRepository.findByUsername(username).isPresent();
    }

    /**
     * Check if an email already exists in the system
     *
     * @param email The email to check
     * @return true if the email exists, false otherwise
     */
    public boolean emailExists(String email) {
        log.debug("Checking if email exists: {}", email);
        return userRepository.findByEmail(email).isPresent();
    }

    /**
     * Delete a user by ID - used for rollback in case of distributed transaction failures
     *
     * @param userId The ID of the user to delete
     * @return true if deletion was successful, false otherwise
     */
    public boolean deleteUser(Long userId) {
        log.info("Deleting user with ID: {}", userId);
        try {
            if (!userRepository.existsById(userId)) {
                log.warn("Cannot delete user with ID: {} - not found", userId);
                return false;
            }
            
            userRepository.deleteById(userId);
            log.info("User with ID: {} deleted successfully", userId);
            return true;
        } catch (Exception e) {
            log.error("Error deleting user with ID: {}", userId, e);
            return false;
        }
    }

    /**
     * Increment labs solved counter and deduct points
     */
    public OperationResponse incrementLabsSolved(IncrementLabsSolvedRequest request) {
        try {
            userStatsService.incrementLabsSolved(request.getUserId());
            return OperationResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Labs solved count incremented successfully")
                .build();
        } catch (InsufficientBalanceException e) {
            log.warn("Failed to increment labs solved for user {}: {}", request.getUserId(), e.getMessage());
            return OperationResponse.newBuilder()
                .setSuccess(false)
                .setMessage(e.getMessage())
                .build();
        } catch (Exception e) {
            log.error("Error incrementing labs solved for user {}: {}", request.getUserId(), e.getMessage(), e);
            return OperationResponse.newBuilder()
                .setSuccess(false)
                .setMessage("Failed to increment labs solved: " + e.getMessage())
                .build();
        }
    }

    /**
     * Increment labs reviewed counter and add reward points
     */
    public OperationResponse incrementLabsReviewed(IncrementLabsReviewedRequest request) {
        try {
            userStatsService.incrementLabsReviewed(request.getUserId());
            return OperationResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Labs reviewed count incremented successfully")
                .build();
        } catch (Exception e) {
            log.error("Error incrementing labs reviewed for user {}: {}", request.getUserId(), e.getMessage(), e);
            return OperationResponse.newBuilder()
                .setSuccess(false)
                .setMessage("Failed to increment labs reviewed: " + e.getMessage())
                .build();
        }
    }

    public GetUsersCountResponse getUsersCount(GetUsersCountRequest request) {
        log.info("Getting total count of users");
        try {
            long count = userRepository.count();
            log.info("Total users count: {}", count);
            return GetUsersCountResponse.newBuilder()
                .setCount((int) count)
                .build();
        } catch (Exception e) {
            log.error("Error getting users count", e);
            throw new RuntimeException("Failed to get users count: " + e.getMessage());
        }
    }

    private UserInfo buildUserInfo(User user) {
        return UserInfo.newBuilder()
            .setUserId(user.getId())
            .setUsername(user.getUsername())
            .setFirstName(user.getFirstName())
            .setLastName(user.getLastName())
            .setRole(user.getRole().name())
            .setEmail(user.getEmail())
            .setLabsSolved(user.getLabsSolved())
            .setLabsReviewed(user.getLabsReviewed())
            .setBalance(user.getBalance())
            .build();
    }

    private UserProfileResponse buildUserProfileResponse(User user) {
        return UserProfileResponse.newBuilder()
            .setUserInfo(buildUserInfo(user))
            .setStatus("ACTIVE")
            .build();
    }
}
