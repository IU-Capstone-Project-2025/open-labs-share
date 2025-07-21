package olsh.backend.api_gateway.grpc.client;

import io.grpc.Channel;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import olsh.backend.api_gateway.exception.GrpcError;
import olsh.backend.api_gateway.exception.UserNotFoundException;
import olsh.backend.api_gateway.grpc.proto.UsersServiceGrpc;
import olsh.backend.api_gateway.grpc.proto.UsersServiceProto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UserServiceClient {

    private final UsersServiceGrpc.UsersServiceBlockingStub userServiceStub;

    @Autowired
    public UserServiceClient(GrpcChannelFactory channelFactory) {
        Channel channel = channelFactory.createChannel("user-service");
        this.userServiceStub = UsersServiceGrpc.newBlockingStub(channel);
    }

    /**
     * Retrieves user information by user ID.
     *
     * @param userId The ID of the user to retrieve
     * @return UserInfo object containing user details
     * @throws UserNotFoundException if the user does not exist
     */
    public UserInfo getUser(Long userId) {
        log.debug("Getting user data via gRPC call to user service for userId: {}", userId);
        GetUserInfoRequest request = GetUserInfoRequest.newBuilder()
                .setUserId(userId)
                .build();
        UserInfoResponse response;
        try {
            response = userServiceStub.getUserInfo(request);
        } catch (Exception e) {
            if (e.getMessage().contains("NOT_FOUND")) {
                throw new UserNotFoundException(String.format("User with id=%d not found", userId));
            }
            throw e;
        }
        log.debug("User data response received: userId={}", response.getUserInfo().getUserId());
        return response.getUserInfo();
    }

    /**
     * Increments the number of labs solved for a user.
     *
     * @param id The ID of the user
     * @return OperationResponse indicating success or failure
     */
    public OperationResponse incrementLabsSolvedRequest(Long id) {
        log.debug("Incrementing labs solved for userId: {}", id);
        IncrementLabsSolvedRequest request = IncrementLabsSolvedRequest.newBuilder().setUserId(id).build();
        OperationResponse response = userServiceStub.incrementLabsSolved(request);
        log.debug("Increment labs solved response received for userId: {} with success: {} and message: {}",
                id, response.getSuccess(), response.getMessage());
        return response;
    }

    /**
     * Increments the number of labs reviewed for a user.
     *
     * @param id The ID of the user
     * @return OperationResponse indicating success or failure
     */
    public OperationResponse incrementLabsReviewedRequest(Long id) {
        log.debug("Incrementing labs reviewed for userId: {}", id);
        IncrementLabsReviewedRequest request = IncrementLabsReviewedRequest.newBuilder().setUserId(id).build();
        OperationResponse response = userServiceStub.incrementLabsReviewed(request);
        log.debug("Increment labs reviewed response received for userId: {} with success: {} and message: {}",
                id, response.getSuccess(), response.getMessage());
        return response;
    }

    public Integer usersCount() {
        log.debug("Getting users count via gRPC call to user service");
        try{
            GetUsersCountResponse response = userServiceStub.getUsersCount(GetUsersCountRequest.newBuilder().build());
            log.debug("Users count response received: {}", response.getCount());
            return response.getCount();
        } catch (StatusRuntimeException e) {
            log.error("Error calling GetArticlesCount gRPC: {}", e.getMessage(), e);
            throw new GrpcError(HttpStatus.INTERNAL_SERVER_ERROR, e.getStatus().getCode().name(), e.getMessage());
        }
    }
}

