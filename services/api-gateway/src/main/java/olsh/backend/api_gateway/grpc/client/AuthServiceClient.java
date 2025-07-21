package olsh.backend.api_gateway.grpc.client;

import io.grpc.Channel;
import lombok.extern.slf4j.Slf4j;
import olsh.backend.api_gateway.grpc.model.AuthValidationResponse;
import olsh.backend.api_gateway.grpc.proto.AuthServiceGrpc;
import olsh.backend.api_gateway.grpc.proto.AuthServiceProto;
import olsh.backend.api_gateway.grpc.proto.AuthServiceProto.*;
import olsh.backend.api_gateway.grpc.model.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AuthServiceClient {

    private final AuthServiceGrpc.AuthServiceBlockingStub authServiceStub;

    @Autowired
    public AuthServiceClient(GrpcChannelFactory channelFactory) {
        Channel channel = channelFactory.createChannel("auth-service");
        this.authServiceStub = AuthServiceGrpc.newBlockingStub(channel);
    }

    /**
     * Validates a JWT token by calling the auth service.
     *
     * @param token The JWT token to validate
     * @return AuthValidationResponse containing validation result and user info
     */
    public AuthValidationResponse validateToken(String token) {
        log.debug("Validating token via gRPC call to auth service");
        ValidateTokenRequest request = ValidateTokenRequest.newBuilder()
                .setToken(token)
                .build();
        ValidateTokenResponse response = authServiceStub.validateToken(request);
        log.debug("Token validation response received for user {} with result {}",
                response.getUserInfo().getUsername(), response.getValid());
        // Convert gRPC response to our model
        UserInfo userInfo = null;
        if (response.hasUserInfo()) {
            // Get gRPC response
            AuthServiceProto.UserInfo grpcUserInfo = response.getUserInfo();

            userInfo = new UserInfo(
                    grpcUserInfo.getUserId(),
                    grpcUserInfo.getUsername(),
                    grpcUserInfo.getFirstName(),
                    grpcUserInfo.getLastName(),
                    grpcUserInfo.getRole(),
                    grpcUserInfo.getEmail(),
                    grpcUserInfo.getLabsSolved(),
                    grpcUserInfo.getLabsReviewed(),
                    grpcUserInfo.getBalance()
            );
        }
        return new AuthValidationResponse(
                response.getValid(),
                userInfo,
                response.getExpirationTime(),
                response.getErrorMessage()
        );
    }
}

