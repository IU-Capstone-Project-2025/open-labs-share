package olsh.backend.marimoservice.service;

import com.olsh.auth.proto.AuthServiceGrpc;
import com.olsh.auth.proto.ValidateTokenRequest;
import com.olsh.auth.proto.ValidateTokenResponse;
import net.devh.boot.grpc.client.inject.GrpcClient;
import olsh.backend.marimoservice.model.dto.AuthValidationResponse;
import olsh.backend.marimoservice.model.dto.UserInfo;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @GrpcClient("auth-service")
    private AuthServiceGrpc.AuthServiceBlockingStub authServiceBlockingStub;

    public AuthValidationResponse validateToken(String token) {
        ValidateTokenRequest request = ValidateTokenRequest.newBuilder()
                .setToken(token)
                .build();
        
        ValidateTokenResponse response = authServiceBlockingStub.validateToken(request);

        UserInfo userInfo = null;
        if (response.getValid()) {
            com.olsh.auth.proto.UserInfo protoUserInfo = response.getUserInfo();
            userInfo = new UserInfo(
                protoUserInfo.getUserId(),
                protoUserInfo.getUsername(),
                protoUserInfo.getFirstName(),
                protoUserInfo.getLastName(),
                protoUserInfo.getRole(),
                protoUserInfo.getEmail(),
                protoUserInfo.getLabsSolved(),
                protoUserInfo.getLabsReviewed(),
                protoUserInfo.getBalance()
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