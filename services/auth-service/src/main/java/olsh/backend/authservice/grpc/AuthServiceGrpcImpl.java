package olsh.backend.authservice.grpc;

import java.time.LocalDateTime;

import com.olsh.auth.proto.AuthServiceGrpc;
import com.olsh.auth.proto.HealthCheckRequest;
import com.olsh.auth.proto.HealthCheckResponse;
import com.olsh.auth.proto.UserInfo;
import com.olsh.auth.proto.ValidateTokenRequest;
import com.olsh.auth.proto.ValidateTokenResponse;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import olsh.backend.authservice.dto.TokenValidationResponse;
import olsh.backend.authservice.service.AuthenticationService;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceGrpcImpl extends AuthServiceGrpc.AuthServiceImplBase {

    private final AuthenticationService authenticationService;
    
    @Override
    public void validateToken(ValidateTokenRequest request, StreamObserver<ValidateTokenResponse> responseObserver) {
        log.debug("gRPC token validation request");
        
        try {
            // Create DTO request object for the existing service method
            olsh.backend.authservice.dto.ValidateTokenRequest dtoRequest = 
                new olsh.backend.authservice.dto.ValidateTokenRequest();
            dtoRequest.setToken(request.getToken());
            
            // Call existing service method
            TokenValidationResponse serviceResponse = authenticationService.validateToken(dtoRequest);
            
            // Convert to gRPC response
            ValidateTokenResponse.Builder responseBuilder = ValidateTokenResponse.newBuilder()
                .setValid(serviceResponse.isValid());
            
            if (serviceResponse.getErrorMessage() != null) {
                responseBuilder.setErrorMessage(serviceResponse.getErrorMessage());
            }
            
            if (serviceResponse.getUserInfo() != null) {
                olsh.backend.authservice.dto.UserInfo userInfo = serviceResponse.getUserInfo();
                UserInfo grpcUserInfo = buildGrpcUserInfo(userInfo);
                responseBuilder.setUserInfo(grpcUserInfo);
            }
            
            if (serviceResponse.getExpirationTime() != null) {
                responseBuilder.setExpirationTime(serviceResponse.getExpirationTime());
            }
            
            ValidateTokenResponse response = responseBuilder.build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("Error in gRPC token validation: {}", e.getMessage(), e);
            ValidateTokenResponse errorResponse = ValidateTokenResponse.newBuilder()
                .setValid(false)
                .setErrorMessage("Token validation failed")
                .build();
            responseObserver.onNext(errorResponse);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void healthCheck(HealthCheckRequest request, StreamObserver<HealthCheckResponse> responseObserver) {
        log.debug("gRPC health check request");
        
        try {
            HealthCheckResponse.HealthData healthData = HealthCheckResponse.HealthData.newBuilder()
                .setTimestamp(LocalDateTime.now().toString())
                .setService("auth-service")
                .setVersion("1.0.0")
                .build();
            
            HealthCheckResponse response = HealthCheckResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Auth service is healthy")
                .setData(healthData)
                .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("Error in gRPC health check: {}", e.getMessage(), e);
            HealthCheckResponse errorResponse = HealthCheckResponse.newBuilder()
                .setSuccess(false)
                .setMessage("Health check failed: " + e.getMessage())
                .build();
            responseObserver.onNext(errorResponse);
            responseObserver.onCompleted();
        }
    }

    /**
     * Converts DTO UserInfo to protobuf UserInfo
     */
    private UserInfo buildGrpcUserInfo(olsh.backend.authservice.dto.UserInfo userInfo) {
        UserInfo.Builder builder = UserInfo.newBuilder()
            .setUserId(userInfo.getUserId());
        
        // Only set string fields if they're not null (protobuf default is empty string)
        if (userInfo.getUsername() != null) {
            builder.setUsername(userInfo.getUsername());
        }
        if (userInfo.getFirstName() != null) {
            builder.setFirstName(userInfo.getFirstName());
        }
        if (userInfo.getLastName() != null) {
            builder.setLastName(userInfo.getLastName());
        }
        if (userInfo.getRole() != null) {
            builder.setRole(userInfo.getRole());
        }
        if (userInfo.getEmail() != null) {
            builder.setEmail(userInfo.getEmail());
        }
        
        // Only set integer fields if they're not null (protobuf default is 0)
        if (userInfo.getLabsSolved() != null) {
            builder.setLabsSolved(userInfo.getLabsSolved());
        }
        if (userInfo.getLabsReviewed() != null) {
            builder.setLabsReviewed(userInfo.getLabsReviewed());
        }
        if (userInfo.getBalance() != null) {
            builder.setBalance(userInfo.getBalance());
        }
        
        return builder.build();
    }
} 