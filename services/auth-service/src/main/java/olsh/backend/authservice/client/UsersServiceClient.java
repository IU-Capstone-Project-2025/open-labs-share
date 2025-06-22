package olsh.backend.authservice.client;

import java.util.concurrent.TimeUnit;

import com.olsh.users.proto.AuthenticateUserRequest;
import com.olsh.users.proto.CreateUserRequest;
import com.olsh.users.proto.DeleteUserRequest;
import com.olsh.users.proto.DeleteUserResponse;
import com.olsh.users.proto.ExistsResponse;
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
import com.olsh.users.proto.UsersServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UsersServiceClient {

    private final ManagedChannel channel;
    private final UsersServiceGrpc.UsersServiceBlockingStub blockingStub;

    public UsersServiceClient(
        @Value("${grpc.users-service.host}") String host,
        @Value("${grpc.users-service.port}") int port) {
        this.channel = ManagedChannelBuilder.forAddress(host, port)
            .usePlaintext()
            .build();

        this.blockingStub = UsersServiceGrpc.newBlockingStub(channel);
        log.info("Initialized UsersServiceClient with connection to {}:{}", host, port);
    }

    public UserProfileResponse getUserProfile(Long userId) {
        log.debug("Sending getUserProfile request for userId: {}", userId);
        GetUserProfileRequest request = GetUserProfileRequest.newBuilder()
            .setUserId(userId)
            .build();

        return blockingStub.withDeadlineAfter(5, TimeUnit.SECONDS).getUserProfile(request);
    }

    public UserProfileResponse updateUserProfile(Long userId,
                                                 String firstName,
                                                 String lastName,
                                                 String email,
                                                 String username,
                                                 String password) {
        log.debug("Sending updateUserProfile request for userId: {}", userId);
        UpdateUserProfileRequest.Builder builder = UpdateUserProfileRequest.newBuilder()
            .setUserId(userId)
            .setFirstName(firstName != null ? firstName : "")
            .setLastName(lastName != null ? lastName : "")
            .setEmail(email != null ? email : "")
            .setUsername(username != null ? username : "");

        if (password != null && !password.isEmpty()) {
            builder.setPassword(password);
        }

        return blockingStub.withDeadlineAfter(5, TimeUnit.SECONDS)
            .updateUserProfile(builder.build());
    }

    public SearchUsersResponse searchUsers(String query, int page, int size) {
        log.debug("Sending searchUsers request with query: {}", query);
        SearchUsersRequest request = SearchUsersRequest.newBuilder()
            .setQuery(query)
            .setPage(page)
            .setSize(size)
            .build();

        return blockingStub.withDeadlineAfter(5, TimeUnit.SECONDS).searchUsers(request);
    }

    public UserInfoResponse getUserInfo(Long userId) {
        log.debug("Sending getUserInfo request for userId: {}", userId);
        GetUserInfoRequest request = GetUserInfoRequest.newBuilder()
            .setUserId(userId)
            .build();

        return blockingStub.withDeadlineAfter(5, TimeUnit.SECONDS).getUserInfo(request);
    }

    public UserInfoResponse findUserByEmail(String email) {
        log.debug("Sending findUserByEmail request for email: {}", email);
        FindUserByEmailRequest request = FindUserByEmailRequest.newBuilder()
            .setEmail(email)
            .build();

        return blockingStub.withDeadlineAfter(5, TimeUnit.SECONDS).findUserByEmail(request);
    }

    public UserInfoResponse findUserByUsername(String username) {
        log.debug("Sending findUserByUsername request for username: {}", username);
        FindUserByUsernameRequest request = FindUserByUsernameRequest.newBuilder()
            .setUsername(username)
            .build();

        return blockingStub.withDeadlineAfter(5, TimeUnit.SECONDS).findUserByUsername(request);
    }

    public UserInfoResponse authenticateUser(String usernameOrEmail,
                                             String password,
                                             boolean usingEmail) {
        log.debug("Sending authenticateUser request");
        AuthenticateUserRequest request = AuthenticateUserRequest.newBuilder()
            .setUsername(usernameOrEmail)
            .setPassword(password)
            .setUsingEmail(usingEmail)
            .build();

        return blockingStub.withDeadlineAfter(5, TimeUnit.SECONDS).authenticateUser(request);
    }

    public UpdatePasswordResponse updatePassword(Long userId,
                                                 String currentPassword,
                                                 String newPassword) {
        log.debug("Sending updatePassword request for userId: {}", userId);
        UpdatePasswordRequest request = UpdatePasswordRequest.newBuilder()
            .setUserId(userId)
            .setCurrentPassword(currentPassword != null ? currentPassword : "")
            .setNewPassword(newPassword)
            .build();

        return blockingStub.withDeadlineAfter(5, TimeUnit.SECONDS).updatePassword(request);
    }

    public UpdateUserLastLoginResponse updateUserLastLogin(Long userId) {
        log.debug("Sending updateUserLastLogin request for userId: {}", userId);
        UpdateUserLastLoginRequest request = UpdateUserLastLoginRequest.newBuilder()
            .setUserId(userId)
            .build();

        return blockingStub.withDeadlineAfter(5, TimeUnit.SECONDS).updateUserLastLogin(request);
    }

    public UserProfileResponse createUser(
        String username,
        String firstName,
        String lastName,
        String email,
        olsh.backend.authservice.entity.Role role,
        String password) {
        log.debug("Sending createUser request for username: {}", username);
        CreateUserRequest request = CreateUserRequest.newBuilder()
            .setUsername(username)
            .setFirstName(firstName)
            .setLastName(lastName)
            .setEmail(email)
            .setRole(role.name()) // Use enum's name method to get the string value
            .setPassword(password)
            .build();

        return blockingStub.withDeadlineAfter(5, TimeUnit.SECONDS).createUser(request);
    }

    /**
     * Checks if a username already exists
     * This is a non-exception based method that returns a boolean
     * 
     * @param username The username to check
     * @return true if username exists, false if it doesn't
     * @throws Exception if there's a system error checking the username
     */
    public boolean isUsernameExists(String username) {
        log.debug("Checking if username exists: {}", username);
        try {
            FindUserByUsernameRequest request = FindUserByUsernameRequest.newBuilder()
                .setUsername(username)
                .build();
                
            // Use the dedicated RPC endpoint for checking username existence
            ExistsResponse response = blockingStub.withDeadlineAfter(5, TimeUnit.SECONDS)
                .checkUsernameExists(request);
            
            log.debug("Username '{}' exists: {}", username, response.getExists());
            return response.getExists();
        } catch (Exception e) {
            log.error("Error checking username existence: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * Checks if an email already exists
     * This is a non-exception based method that returns a boolean
     * 
     * @param email The email to check
     * @return true if email exists, false if it doesn't
     * @throws Exception if there's a system error checking the email
     */
    public boolean isEmailExists(String email) {
        log.debug("Checking if email exists: {}", email);
        try {
            FindUserByEmailRequest request = FindUserByEmailRequest.newBuilder()
                .setEmail(email)
                .build();
                
            // Use the dedicated RPC endpoint for checking email existence
            ExistsResponse response = blockingStub.withDeadlineAfter(5, TimeUnit.SECONDS)
                .checkEmailExists(request);
            
            log.debug("Email '{}' exists: {}", email, response.getExists());
            return response.getExists();
        } catch (Exception e) {
            log.error("Error checking email existence: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Delete a user from the users-service database
     * Used for rollback in case of distributed transaction failures
     * 
     * @param userId The ID of the user to delete
     * @return true if deletion was successful, false otherwise
     */
    public boolean deleteUser(Long userId) {
        log.debug("Sending deleteUser request for userId: {}", userId);
        try {
            DeleteUserRequest request = DeleteUserRequest.newBuilder()
                .setUserId(userId)
                .build();
                
            DeleteUserResponse response = blockingStub.withDeadlineAfter(5, TimeUnit.SECONDS)
                .deleteUser(request);
            
            log.debug("Delete user response success: {}, message: {}", 
                      response.getSuccess(), response.getMessage());
            return response.getSuccess();
        } catch (Exception e) {
            log.error("Error deleting user: {}", e.getMessage());
            return false;
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down UsersServiceClient");
        try {
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.warn("Error shutting down UsersServiceClient", e);
            Thread.currentThread().interrupt();
        }
    }
}
