package olsh.backend.api_gateway.grpc.client;

import com.olsh.users.proto.GetUserInfoRequest;
import com.olsh.users.proto.UserInfoResponse;
import com.olsh.users.proto.UsersServiceGrpc;
import io.grpc.Channel;
import lombok.extern.slf4j.Slf4j;
import olsh.backend.api_gateway.exception.UserNotFoundException;
import olsh.backend.api_gateway.grpc.model.UserData;
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
    
    public UserData getUser(Long userId) {
        log.debug("Getting user data via gRPC call to user service for userId: {}", userId);

        GetUserInfoRequest request = GetUserInfoRequest.newBuilder()
                .setUserId(userId)
                .build();

        UserInfoResponse response;
        try {
            response = userServiceStub.getUserInfo(request);
        }catch (Exception e){
            if (e.getMessage().contains("NOT_FOUND")){
                throw new UserNotFoundException(String.format("User with id=%d not found", userId));
            }
            throw e;
        }

        log.debug("User data response received: userId={}", response.getUserInfo().getUserId());

        return new UserData(
                response.getUserInfo().getUserId(),
                response.getUserInfo().getUsername(),
                response.getUserInfo().getFirstName(),
                response.getUserInfo().getLastName(),
                response.getUserInfo().getEmail(),
                true
        );
    }
}

