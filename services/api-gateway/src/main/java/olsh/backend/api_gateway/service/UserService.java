package olsh.backend.api_gateway.service;

import lombok.extern.slf4j.Slf4j;
import olsh.backend.api_gateway.dto.response.UserResponse;
import olsh.backend.api_gateway.exception.UserNotFoundException;
import olsh.backend.api_gateway.grpc.client.UserServiceClient;
import olsh.backend.api_gateway.grpc.model.UserData;
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

    public UserResponse getUserById(Long userId) {
        log.debug("Getting user data for userId: {}", userId);

        UserData userData = userServiceClient.getUser(userId);

        if (!userData.isFound()) {
            throw new UserNotFoundException("User not found with id: " + userId);
        }

        log.debug("User data retrieved successfully for userId: {}", userId);

        return new UserResponse(
                userData.getId(),
                userData.getUsername(),
                userData.getName(),
                userData.getSurname(),
                userData.getEmail()
        );
    }
}
