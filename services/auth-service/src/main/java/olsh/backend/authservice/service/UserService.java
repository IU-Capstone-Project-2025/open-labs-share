package olsh.backend.authservice.service;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.olsh.users.proto.UserInfoResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import olsh.backend.authservice.client.UsersServiceClient;
import olsh.backend.authservice.entity.Role;
import olsh.backend.authservice.entity.User;
import olsh.backend.authservice.exception.NotFoundException;



@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UsersServiceClient usersServiceClient;

    /**
     * Loads user by username from users-service via gRPC
     */
    public User getByUsername(String username) {
        try {
            log.debug("Loading user by username: {}", username);
            UserInfoResponse response = usersServiceClient.findUserByUsername(username);
            return mapToUser(response);
        } catch (Exception e) {
            log.error("Failed to load user by username: {}", username, e);
            throw new UsernameNotFoundException("User not found: " + username);
        }
    }

    /**
     * Loads user by email from users-service via gRPC
     */
    public User getByEmail(String email) {
        try {
            log.debug("Loading user by email: {}", email);
            UserInfoResponse response = usersServiceClient.findUserByEmail(email);
            return mapToUser(response);
        } catch (Exception e) {
            log.error("Failed to load user by email: {}", email, e);
            throw new UsernameNotFoundException("User not found: " + email);
        }
    }

    /**
     * Loads user by ID from users-service via gRPC
     */
    public User getById(Long userId) {
        try {
            log.debug("Loading user by ID: {}", userId);
            UserInfoResponse response = usersServiceClient.getUserInfo(userId);
            return mapToUser(response);
        } catch (Exception e) {
            log.error("Failed to load user by ID: {}", userId, e);
            throw new NotFoundException("User not found: " + userId);
        }
    }

    /**
     * Checks if username exists in users-service
     */
    public boolean existsByUsername(String username) {
        try {
            return usersServiceClient.isUsernameExists(username);
        } catch (Exception e) {
            log.error("Failed to check username existence: {}", username, e);
            return false;
        }
    }

    /**
     * Checks if email exists in users-service
     */
    public boolean existsByEmail(String email) {
        try {
            return usersServiceClient.isEmailExists(email);
        } catch (Exception e) {
            log.error("Failed to check email existence: {}", email, e);
            return false;
        }
    }

    /**
     * Provides UserDetailsService implementation for Spring Security
     */
    public UserDetailsService userDetailsService() {
        return this::getByUsername;
    }

    /**
     * Gets current authenticated user
     */
    public User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return getByUsername(username);
    }

    /**
     * Maps gRPC UserInfoResponse to User domain object
     */
    private User mapToUser(UserInfoResponse response) {
        var userInfo = response.getUserInfo();
        
        return User.builder()
                .userId(userInfo.getUserId())
                .username(userInfo.getUsername())
                .email(userInfo.getEmail())
                .firstName(userInfo.getFirstName())
                .lastName(userInfo.getLastName())
                .role(Role.valueOf(userInfo.getRole()))
                .build();
    }
}
