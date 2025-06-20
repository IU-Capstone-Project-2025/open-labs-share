package olsh.backend.api_gateway.grpc.client;

import io.grpc.Channel;
import lombok.extern.slf4j.Slf4j;
import olsh.backend.api_gateway.exception.UserNotFoundException;
import olsh.backend.api_gateway.grpc.model.UserData;
import olsh.backend.api_gateway.grpc.proto.GetUserRequest;
import olsh.backend.api_gateway.grpc.proto.GetUserResponse;
import olsh.backend.api_gateway.grpc.proto.UserServiceGrpc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UserServiceClient {

    private final UserServiceGrpc.UserServiceBlockingStub userServiceStub;

    @Autowired
    public UserServiceClient(GrpcChannelFactory channelFactory) {
        Channel channel = channelFactory.createChannel("user-service");
        this.userServiceStub = UserServiceGrpc.newBlockingStub(channel);
    }
    public UserData getUser(Long userId) {
        log.debug("Getting user data via gRPC call to user service for userId: {}", userId);

        GetUserRequest request = GetUserRequest.newBuilder()
                .setUserId(userId)
                .build();

        GetUserResponse response;
        try {
            response = userServiceStub.getUser(request);
        }catch (Exception e){
            if (e.getMessage().contains("NOT_FOUND")){
                throw new UserNotFoundException(String.format("User with id=%d not found", userId));
            }
            throw e;
        }

        log.debug("User data response received: userId={}", response.getId());

        return new UserData(
                response.getId(),
                response.getUsername(),
                response.getName(),
                response.getSurname(),
                response.getEmail(),
                true
        );
    }
}

