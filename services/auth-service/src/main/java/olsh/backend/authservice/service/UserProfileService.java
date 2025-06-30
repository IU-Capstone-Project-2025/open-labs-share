package olsh.backend.authservice.service;

import org.springframework.stereotype.Service;

import com.olsh.users.proto.UserProfileResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import olsh.backend.authservice.client.UsersServiceClient;
import olsh.backend.authservice.dto.UserInfo;
import olsh.backend.authservice.dto.UserProfileResponseWithUserInfo;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileService {
    
    private final UsersServiceClient usersServiceClient;

    public UserProfileResponseWithUserInfo getUserProfileWithUserInfo(String username) {
        // Get user from users-service directly
        log.debug("Fetching profile for user: {}", username);

        try {
            // Get user info first to get the userId
            var userInfoResponse = usersServiceClient.findUserByUsername(username);
            var userInfo = userInfoResponse.getUserInfo();
            
            // Get full profile using userId
            UserProfileResponse usersResponse = usersServiceClient.getUserProfile(userInfo.getUserId());

            UserInfo mappedUserInfo = UserInfo.builder()
                .userId(userInfo.getUserId())
                .username(userInfo.getUsername())
                .firstName(usersResponse.getUserInfo().getFirstName())
                .lastName(usersResponse.getUserInfo().getLastName())
                .role(userInfo.getRole())
                .email(usersResponse.getUserInfo().getEmail())
                .labsSolved(usersResponse.getUserInfo().getLabsSolved())
                .labsReviewed(usersResponse.getUserInfo().getLabsReviewed())
                .balance(usersResponse.getUserInfo().getBalance())
                .build();
                
            return UserProfileResponseWithUserInfo.builder()
                .userInfo(mappedUserInfo)
                .status(usersResponse.getStatus())
                .build();
        } catch (Exception e) {
            log.error("Error getting user profile from users-service for username: {}", username, e);
            throw new RuntimeException("Failed to get user profile", e);
        }
    }

    public UserInfo getUserInfo(Long userId) {
        try {
            com.olsh.users.proto.UserInfoResponse response = usersServiceClient.getUserInfo(userId);
            return UserInfo.builder()
                .userId(userId)
                .username(response.getUserInfo().getUsername())
                .firstName(response.getUserInfo().getFirstName())
                .lastName(response.getUserInfo().getLastName())
                .role(response.getUserInfo().getRole())
                .email(response.getUserInfo().getEmail())
                .labsSolved(response.getUserInfo().getLabsSolved())
                .labsReviewed(response.getUserInfo().getLabsReviewed())
                .balance(response.getUserInfo().getBalance())
                .build();
        } catch (Exception e) {
            log.error("Error getting user info from users-service for ID: {}", userId, e);
            throw e;
        }
    }

    public UserInfo findUserByEmail(String email) {
        try {
            com.olsh.users.proto.UserInfoResponse response =
                usersServiceClient.findUserByEmail(email);
            if (response == null) {
                return null;
            }
            return UserInfo.builder()
                .userId(response.getUserInfo().getUserId())
                .username(response.getUserInfo().getUsername())
                .firstName(response.getUserInfo().getFirstName())
                .lastName(response.getUserInfo().getLastName())
                .role(response.getUserInfo().getRole())
                .email(response.getUserInfo().getEmail())
                .labsSolved(response.getUserInfo().getLabsSolved())
                .labsReviewed(response.getUserInfo().getLabsReviewed())
                .balance(response.getUserInfo().getBalance())
                .build();
        } catch (Exception e) {
            log.error("Error finding user by email in users-service: {}", email, e);
            return null;
        }
    }
}
