package olsh.backend.api_gateway.service;

import lombok.extern.slf4j.Slf4j;
import olsh.backend.api_gateway.dto.response.UserResponse;
import olsh.backend.api_gateway.exception.UserNotFoundException;
import olsh.backend.api_gateway.grpc.client.UserServiceClient;
import olsh.backend.api_gateway.grpc.proto.UsersServiceProto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UserService {

    private final UserServiceClient userServiceClient;

    @Autowired
    public UserService(UserServiceClient userServiceClient) {
        this.userServiceClient = userServiceClient;
    }

    public UserResponse getUserById(Long id) {
        log.debug("Getting user data for userId: {}", id);

        UsersServiceProto.UserInfo user = userServiceClient.getUser(id);

        log.debug("User data retrieved successfully for userId: {}", id);

        return mapUserInfoToUserResponse(user);
    }

    public UserResponse getUserByIdSafe(Long id) {
        log.debug("Getting user data for userId: {}", id);

        try {
            UsersServiceProto.UserInfo user = userServiceClient.getUser(id);
            return mapUserInfoToUserResponse(user);
        } catch (UserNotFoundException e) {
            log.warn("User with id={} not found", id);
            return new UserResponse(id, "Username", "User", "Not Found", "email", 0, 0, 0);
        }
    }

    public UserResponse incrementLabsSolved(Long id) {
        UsersServiceProto.OperationResponse response = userServiceClient.incrementLabsSolvedRequest(id);
        if (!response.getSuccess()) {
            log.error("Failed to increment labs solved for userId: {}. Message: {}", id, response.getMessage());
            throw new RuntimeException("Failed to increment labs solved");
        }
        return getUserById(id);
    }

    public UserResponse incrementLabsReviewed(Long id) {
        UsersServiceProto.OperationResponse response = userServiceClient.incrementLabsReviewedRequest(id);
        if (!response.getSuccess()) {
            log.error("Failed to increment labs reviewed for userId: {}. Message: {}", id, response.getMessage());
            throw new RuntimeException("Failed to increment labs reviewed");
        }
        return getUserById(id);
    }

    private UserResponse mapUserInfoToUserResponse(UsersServiceProto.UserInfo userInfo) {
        return new UserResponse(
                userInfo.getUserId(),
                userInfo.getUsername(),
                userInfo.getFirstName(),
                userInfo.getLastName(),
                userInfo.getEmail(),
                userInfo.getLabsSolved(),
                userInfo.getLabsReviewed(),
                userInfo.getBalance()
        );
    }
}
