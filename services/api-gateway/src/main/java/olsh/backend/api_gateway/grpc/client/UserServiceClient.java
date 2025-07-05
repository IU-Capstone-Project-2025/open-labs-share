package olsh.backend.api_gateway.grpc.client;

import io.grpc.Channel;
import lombok.extern.slf4j.Slf4j;
import olsh.backend.api_gateway.exception.UserNotFoundException;
import olsh.backend.api_gateway.grpc.proto.UsersServiceGrpc;
import olsh.backend.api_gateway.grpc.proto.UsersServiceProto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.grpc.client.GrpcChannelFactory;
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

    public OperationResponse incrementLabsSolvedRequest(Long id) {
        log.debug("Incrementing labs solved for userId: {}", id);
        IncrementLabsSolvedRequest request = IncrementLabsSolvedRequest.newBuilder().setUserId(id).build();
        OperationResponse response = userServiceStub.incrementLabsSolved(request);
        log.debug("Increment labs solved response received for userId: {} with success: {} and message: {}",
                id, response.getSuccess(), response.getMessage());
        return response;
    }

    public OperationResponse incrementLabsReviewedRequest(Long id) {
        log.debug("Incrementing labs reviewed for userId: {}", id);
        IncrementLabsReviewedRequest request = IncrementLabsReviewedRequest.newBuilder().setUserId(id).build();
        OperationResponse response = userServiceStub.incrementLabsReviewed(request);
        log.debug("Increment labs reviewed response received for userId: {} with success: {} and message: {}",
                id, response.getSuccess(), response.getMessage());
        return response;
    }
}

