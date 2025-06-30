package olsh.backend.authservice.service;

import java.time.LocalDateTime;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import olsh.backend.authservice.client.UsersServiceClient;
import olsh.backend.authservice.dto.AuthenticationResponse;

import olsh.backend.authservice.dto.PasswordResetConfirmRequest;
import olsh.backend.authservice.dto.PasswordResetRequest;
import olsh.backend.authservice.dto.RefreshTokenRequest;
import olsh.backend.authservice.dto.SignInRequest;
import olsh.backend.authservice.dto.SignUpRequest;
import olsh.backend.authservice.dto.TokenValidationResponse;
import olsh.backend.authservice.dto.UpdateProfileRequest;
import olsh.backend.authservice.dto.UpdateProfileResponse;
import olsh.backend.authservice.dto.UserInfo;
import olsh.backend.authservice.dto.UserProfileResponseWithUserInfo;
import olsh.backend.authservice.dto.ValidateTokenRequest;
import olsh.backend.authservice.entity.Role;
import olsh.backend.authservice.entity.User;
import olsh.backend.authservice.exception.AuthenticationException;
import olsh.backend.authservice.exception.ValidationException;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final UserService userService;
    private final JwtService jwtService;
    private final UserProfileService userProfileService;
    private final UsersServiceClient usersServiceClient;

    @Value("${token.access.expiration}")
    private long ACCESS_TOKEN_EXPIRATION_TIME;

    public AuthenticationResponse signUp(SignUpRequest request) {
        try {
            checkUsernameAvailability(request.getUsername());
            checkEmailAvailability(request.getEmail());
            var userProfileResponse = usersServiceClient.createUser(
                request.getUsername(),
                request.getFirstName(),
                request.getLastName(),
                request.getEmail(),
                Role.ROLE_USER,
                request.getPassword()
            );
            
            var userInfo = userProfileResponse.getUserInfo();

            User user = User.builder()
                .userId(userInfo.getUserId())
                .username(userInfo.getUsername())
                .email(userInfo.getEmail())
                .firstName(userInfo.getFirstName())
                .lastName(userInfo.getLastName())
                .role(Role.valueOf(userInfo.getRole()))
                .build();

            String accessToken = jwtService.generateToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);

            log.info("User {} registered successfully with ID: {}", user.getUsername(), user.getUserId());
            return buildAuthenticationResponse(user, accessToken, refreshToken);
            
        } catch (Exception e) {
            log.error("Failed to create user: {}", e.getMessage(), e);
            throw new ValidationException("Failed to create user: " + e.getMessage());
        }
    }

    public AuthenticationResponse signIn(SignInRequest request) {
        try {

            var userInfoResponse = usersServiceClient.authenticateUser(
                request.getUsernameOrEmail(),
                request.getPassword(),
                request.getUsernameOrEmail().contains("@")
            );
            
            var userInfo = userInfoResponse.getUserInfo();

            User user = User.builder()
                .userId(userInfo.getUserId())
                .username(userInfo.getUsername())
                .email(userInfo.getEmail())
                .firstName(userInfo.getFirstName())
                .lastName(userInfo.getLastName())
                .role(Role.valueOf(userInfo.getRole()))
                .build();

            usersServiceClient.updateUserLastLogin(userInfo.getUserId());

            String accessToken = jwtService.generateToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);

            log.info("User {} logged in successfully", user.getUsername());
            return buildAuthenticationResponse(user, accessToken, refreshToken);
        } catch (Exception e) {
            log.warn("Failed login attempt for user: {}", request.getUsernameOrEmail(), e);
            throw new AuthenticationException("Invalid credentials");
        }
    }

    public AuthenticationResponse refreshToken(RefreshTokenRequest request) {
        try {
            String username = jwtService.extractUsername(request.getRefreshToken());
            UserDetails userDetails = userService.userDetailsService().loadUserByUsername(username);

            if (jwtService.isTokenValidAndNotBlacklisted(request.getRefreshToken(), userDetails)) {
                User user = (User) userDetails;
                String newAccessToken = jwtService.generateToken(userDetails);
                String newRefreshToken = jwtService.generateRefreshToken(userDetails);

                return buildAuthenticationResponse(user, newAccessToken, newRefreshToken);
            } else {
                throw new AuthenticationException("Invalid refresh token");
            }
        } catch (Exception e) {
            log.error("Error refreshing token: {}", e.getMessage());
            throw new AuthenticationException("Invalid refresh token");
        }
    }

    public TokenValidationResponse validateToken(ValidateTokenRequest request) {
        try {
            String username = jwtService.extractUsername(request.getToken());
            UserDetails userDetails = userService.userDetailsService().loadUserByUsername(username);

            if (jwtService.isTokenBlacklisted(request.getToken())) {
                return TokenValidationResponse.builder()
                    .valid(false)
                    .errorMessage("Token has been invalidated (user logged out)")
                    .build();
            }
            if (jwtService.isTokenValidAndNotBlacklisted(request.getToken(), userDetails)) {
                User user = (User) userDetails;
                Date expiration = jwtService.extractExpiration(request.getToken());

                UserInfo userInfo;
                try {
                    userInfo = userProfileService.getUserInfo(user.getUserId());
                } catch (Exception e) {
                    log.warn("Failed to get user info from users-service, using fallback values", e);
                    userInfo = createFallbackUserInfo(user);
                }

                return TokenValidationResponse.builder()
                    .valid(true)
                    .userInfo(userInfo)
                    .expirationTime(expiration.getTime())
                    .build();
            } else {
                return TokenValidationResponse.builder()
                    .valid(false)
                    .errorMessage("Invalid token")
                    .build();
            }
        } catch (Exception e) {
            log.error("Error validating token: {}", e.getMessage());
            return TokenValidationResponse.builder()
                .valid(false)
                .errorMessage("Token validation failed")
                .build();
        }
    }

    public void logout(String token) {
        jwtService.blacklistToken(token);
        log.info("User logged out successfully");
    }

    public void requestPasswordReset(PasswordResetRequest request) {
        // Implementation would delegate to users-service if needed
        throw new UnsupportedOperationException("Password reset not implemented yet");
    }

    public void confirmPasswordReset(PasswordResetConfirmRequest request) {
        // Implementation would delegate to users-service if needed
        throw new UnsupportedOperationException("Password reset not implemented yet");
    }

    public UserProfileResponseWithUserInfo getUserProfileWithUserInfo(String username) {
        return userProfileService.getUserProfileWithUserInfo(username);
    }

    public void verifyEmail(String token) {
        // Implementation would delegate to users-service if needed
        throw new UnsupportedOperationException("Email verification not implemented yet");
    }

    public UpdateProfileResponse updateProfile(UpdateProfileRequest request, String currentUsername, String currentToken) {
        try {
            User currentUser = userService.getByUsername(currentUsername);
            String originalUsername = currentUser.getUsername();
            
            var userProfileResponse = usersServiceClient.updateUserProfile(
                currentUser.getUserId(),
                request.getFirstName(),
                request.getLastName(),
                request.getEmail(),
                request.getUsername(),
                request.getPassword()
            );
            
            var updatedUserInfo = userProfileResponse.getUserInfo();
            boolean usernameChanged = !originalUsername.equals(updatedUserInfo.getUsername());
            
            String accessToken = null;
            String refreshToken = null;
            LocalDateTime expiresAt = null;
            
            if (usernameChanged) {
                jwtService.blacklistToken(currentToken);
                
                User updatedUser = User.builder()
                    .userId(updatedUserInfo.getUserId())
                    .username(updatedUserInfo.getUsername())
                    .email(updatedUserInfo.getEmail())
                    .firstName(updatedUserInfo.getFirstName())
                    .lastName(updatedUserInfo.getLastName())
                    .role(Role.valueOf(updatedUserInfo.getRole()))
                    .build();
                
                accessToken = jwtService.generateToken(updatedUser);
                refreshToken = jwtService.generateRefreshToken(updatedUser);
                expiresAt = java.time.LocalDateTime.now().plusSeconds(ACCESS_TOKEN_EXPIRATION_TIME / 1000);
                
                log.info("Username changed from '{}' to '{}' for user ID: {}. New tokens generated.", 
                         originalUsername, updatedUserInfo.getUsername(), currentUser.getUserId());
            } else {
                log.info("Profile updated for user ID: {} without username change. No new tokens issued.", currentUser.getUserId());
            }
            
            UserInfo responseUserInfo = UserInfo.builder()
                .userId(updatedUserInfo.getUserId())
                .username(updatedUserInfo.getUsername())
                .firstName(updatedUserInfo.getFirstName())
                .lastName(updatedUserInfo.getLastName())
                .email(updatedUserInfo.getEmail())
                .role(updatedUserInfo.getRole())
                .labsSolved(updatedUserInfo.getLabsSolved())
                .labsReviewed(updatedUserInfo.getLabsReviewed())
                .balance(updatedUserInfo.getBalance())
                .build();
            
            return UpdateProfileResponse.builder()
                .userInfo(responseUserInfo)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresAt(expiresAt)
                .tokenType(usernameChanged ? "Bearer" : null)
                .usernameChanged(usernameChanged)
                .message("Profile updated successfully")
                .build();
                
        } catch (Exception e) {
            log.error("Failed to update profile for user: {}", currentUsername, e);
            throw new ValidationException("Failed to update profile: " + e.getMessage());
        }
    }

    private AuthenticationResponse buildAuthenticationResponse(User user,
                                                               String accessToken,
                                                               String refreshToken) {
        UserInfo userInfo;
        try {
            userInfo = userProfileService.getUserInfo(user.getUserId());
        } catch (Exception e) {
            log.warn("Failed to get complete user info from users-service, using fallback values", e);
            userInfo = createFallbackUserInfo(user);
        }

        return AuthenticationResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .userInfo(userInfo)
            .expiresAt(java.time.LocalDateTime.now().plusSeconds(ACCESS_TOKEN_EXPIRATION_TIME / 1000))
            .tokenType("Bearer")
            .build();
    }

    private void checkUsernameAvailability(String username) throws ValidationException {
        if (usersServiceClient.isUsernameExists(username)) {
            throw new ValidationException("Username already exists");
        }
    }

    private void checkEmailAvailability(String email) throws ValidationException {
        if (usersServiceClient.isEmailExists(email)) {
            throw new ValidationException("Email already exists");
        }
    }

    /**
     * Creates fallback UserInfo when users-service is unavailable
     * ONLY includes static user data - NEVER includes mutable transactional data (points/counters)
     * This ensures data integrity and prevents phantom balance issues
     */
    private UserInfo createFallbackUserInfo(User user) {
        log.info("Creating fallback user info for user ID: {} - points data will be unavailable", user.getUserId());
        return UserInfo.builder()
            .userId(user.getUserId())
            .username(user.getUsername() != null ? user.getUsername() : "")
            .firstName(user.getFirstName() != null ? user.getFirstName() : "")
            .lastName(user.getLastName() != null ? user.getLastName() : "")
            .email(user.getEmail() != null ? user.getEmail() : "")
            .role(user.getRole() != null ? user.getRole().name() : "")
            .labsSolved(0)
            .labsReviewed(0)
            .balance(0)
            .build();
    }
}