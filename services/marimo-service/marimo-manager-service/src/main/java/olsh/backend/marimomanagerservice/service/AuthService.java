package olsh.backend.marimomanagerservice.service;

import lombok.RequiredArgsConstructor;
import olsh.backend.grpc.auth.ValidateTokenResponse;
import olsh.backend.marimomanagerservice.grpc.client.AuthServiceClient;
import olsh.backend.marimomanagerservice.model.dto.AuthValidationResponse;
import olsh.backend.marimomanagerservice.model.dto.UserInfo;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthServiceClient authServiceClient;

    public AuthValidationResponse validateToken(String token) {
        
        ValidateTokenResponse response = authServiceClient.validateToken(token);

        UserInfo userInfo = null;
        if (response.getValid()) {
            olsh.backend.grpc.auth.UserInfo protoUserInfo = response.getUserInfo();
            userInfo = new UserInfo(
                (long) protoUserInfo.getUserId(),
                protoUserInfo.getUsername(),
                protoUserInfo.getFirstName(),
                protoUserInfo.getLastName(),
                protoUserInfo.getRole(),
                protoUserInfo.getEmail(),
                protoUserInfo.getLabsSolved(),
                protoUserInfo.getLabsReviewed(),
                (int) protoUserInfo.getBalance()
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