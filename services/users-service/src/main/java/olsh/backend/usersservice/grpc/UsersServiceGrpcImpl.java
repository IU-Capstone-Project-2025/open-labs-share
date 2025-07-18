package olsh.backend.usersservice.grpc;

import java.time.Instant;

import com.olsh.users.proto.AuthenticateUserRequest;
import com.olsh.users.proto.CreateUserRequest;
import com.olsh.users.proto.DeleteUserRequest;
import com.olsh.users.proto.DeleteUserResponse;
import com.olsh.users.proto.ExistsResponse;
import com.olsh.users.proto.FindUserByEmailRequest;
import com.olsh.users.proto.FindUserByUsernameRequest;
import com.olsh.users.proto.GetUserInfoRequest;
import com.olsh.users.proto.GetUserProfileRequest;
import com.olsh.users.proto.GetUsersCountRequest;
import com.olsh.users.proto.GetUsersCountResponse;
import com.olsh.users.proto.HealthCheckRequest;
import com.olsh.users.proto.HealthCheckResponse;
import com.olsh.users.proto.IncrementLabsReviewedRequest;
import com.olsh.users.proto.IncrementLabsSolvedRequest;
import com.olsh.users.proto.OperationResponse;
import com.olsh.users.proto.SearchUsersRequest;
import com.olsh.users.proto.SearchUsersResponse;
import com.olsh.users.proto.UpdatePasswordRequest;
import com.olsh.users.proto.UpdatePasswordResponse;
import com.olsh.users.proto.UpdateUserLastLoginRequest;
import com.olsh.users.proto.UpdateUserLastLoginResponse;
import com.olsh.users.proto.UpdateUserProfileRequest;
import com.olsh.users.proto.UserInfoResponse;
import com.olsh.users.proto.UserProfileResponse;
import com.olsh.users.proto.UsersServiceGrpc;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import olsh.backend.usersservice.exception.AuthenticationException;
import olsh.backend.usersservice.exception.NotFoundException;
import olsh.backend.usersservice.service.UserService;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class UsersServiceGrpcImpl extends UsersServiceGrpc.UsersServiceImplBase {

    private final UserService userService;

    @Override
    public void getUserProfile(GetUserProfileRequest request,
                               StreamObserver<UserProfileResponse> responseObserver) {
        try {
            log.info("Received GetUserProfile request for user ID: {}", request.getUserId());
            UserProfileResponse response = userService.getUserProfile(request);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (NotFoundException e) {
            log.error("User not found with ID: {}", request.getUserId(), e);
            responseObserver.onError(io.grpc.Status.NOT_FOUND
                                         .withDescription(
                                             "User not found with ID: " + request.getUserId())
                                         .asException());
        } catch (Exception e) {
            log.error("Error getting user profile for ID: {}", request.getUserId(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                                         .withDescription(
                                             "Internal server error: " + e.getMessage())
                                         .asException());
        }
    }

    @Override
    public void updateUserProfile(UpdateUserProfileRequest request,
                                  StreamObserver<UserProfileResponse> responseObserver) {
        try {
            log.info("Received UpdateUserProfile request for user ID: {}", request.getUserId());
            UserProfileResponse response = userService.updateUserProfile(request);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (NotFoundException e) {
            log.error("User not found with ID: {}", request.getUserId(), e);
            responseObserver.onError(io.grpc.Status.NOT_FOUND
                                         .withDescription(
                                             "User not found with ID: " + request.getUserId())
                                         .asException());
        } catch (IllegalArgumentException e) {
            log.error("Invalid update request for user ID: {}", request.getUserId(), e);
            responseObserver.onError(io.grpc.Status.INVALID_ARGUMENT
                                         .withDescription(e.getMessage())
                                         .asException());
        } catch (Exception e) {
            log.error("Error updating user profile for ID: {}", request.getUserId(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                                         .withDescription(
                                             "Internal server error: " + e.getMessage())
                                         .asException());
        }
    }

    @Override
    public void searchUsers(SearchUsersRequest request,
                            StreamObserver<SearchUsersResponse> responseObserver) {
        try {
            log.info("Received SearchUsers request with query: {}", request.getQuery());
            SearchUsersResponse response = userService.searchUsers(request);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error searching users with query: {}", request.getQuery(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                                         .withDescription(
                                             "Internal server error: " + e.getMessage())
                                         .asException());
        }
    }

    @Override
    public void getUserInfo(GetUserInfoRequest request,
                            StreamObserver<UserInfoResponse> responseObserver) {
        try {
            log.info("Received GetUserInfo request for user ID: {}", request.getUserId());
            UserInfoResponse response = userService.getUserInfo(request);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (NotFoundException e) {
            log.error("User not found with ID: {}", request.getUserId(), e);
            responseObserver.onError(io.grpc.Status.NOT_FOUND
                                         .withDescription(
                                             "User not found with ID: " + request.getUserId())
                                         .asException());
        } catch (Exception e) {
            log.error("Error getting user info for ID: {}", request.getUserId(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                                         .withDescription(
                                             "Internal server error: " + e.getMessage())
                                         .asException());
        }
    }

    @Override
    public void findUserByEmail(FindUserByEmailRequest request,
                                StreamObserver<UserInfoResponse> responseObserver) {
        try {
            log.info("Received FindUserByEmail request for email: {}", request.getEmail());
            UserInfoResponse response = userService.findUserByEmail(request);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (NotFoundException e) {
            log.error("User not found with email: {}", request.getEmail(), e);
            responseObserver.onError(io.grpc.Status.NOT_FOUND
                                         .withDescription(
                                             "User not found with email: " + request.getEmail())
                                         .asException());
        } catch (Exception e) {
            log.error("Error finding user by email: {}", request.getEmail(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                                         .withDescription(
                                             "Internal server error: " + e.getMessage())
                                         .asException());
        }
    }

    @Override
    public void findUserByUsername(FindUserByUsernameRequest request,
                                   StreamObserver<UserInfoResponse> responseObserver) {
        try {
            log.info("Received FindUserByUsername request for username: {}", request.getUsername());
            UserInfoResponse response = userService.findUserByUsername(request);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (NotFoundException e) {
            log.error("User not found with username: {}", request.getUsername(), e);
            responseObserver.onError(io.grpc.Status.NOT_FOUND
                                         .withDescription(
                                             "User not found with username: "
                                                 + request.getUsername())
                                         .asException());
        } catch (Exception e) {
            log.error("Error finding user by username: {}", request.getUsername(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                                         .withDescription(
                                             "Internal server error: " + e.getMessage())
                                         .asException());
        }
    }

    @Override
    public void authenticateUser(AuthenticateUserRequest request,
                                 StreamObserver<UserInfoResponse> responseObserver) {
        try {
            log.info("Received AuthenticateUser request");
            UserInfoResponse response = userService.authenticateUser(request);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (NotFoundException e) {
            log.error("Authentication failed: user not found", e);
            responseObserver.onError(io.grpc.Status.NOT_FOUND
                                         .withDescription("User not found")
                                         .asException());
        } catch (AuthenticationException e) {
            log.error("Authentication failed: invalid credentials", e);
            responseObserver.onError(io.grpc.Status.UNAUTHENTICATED
                                         .withDescription("Invalid credentials")
                                         .asException());
        } catch (Exception e) {
            log.error("Error authenticating user", e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                                         .withDescription(
                                             "Internal server error: " + e.getMessage())
                                         .asException());
        }
    }

    @Override
    public void updatePassword(UpdatePasswordRequest request,
                               StreamObserver<UpdatePasswordResponse> responseObserver) {
        try {
            log.info("Received UpdatePassword request for user ID: {}", request.getUserId());
            UpdatePasswordResponse response = userService.updatePassword(request);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (NotFoundException e) {
            log.error("User not found with ID: {}", request.getUserId(), e);
            responseObserver.onError(io.grpc.Status.NOT_FOUND
                                         .withDescription(
                                             "User not found with ID: " + request.getUserId())
                                         .asException());
        } catch (Exception e) {
            log.error("Error updating password for user ID: {}", request.getUserId(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                                         .withDescription(
                                             "Internal server error: " + e.getMessage())
                                         .asException());
        }
    }

    @Override
    public void updateUserLastLogin(UpdateUserLastLoginRequest request,
                                    StreamObserver<UpdateUserLastLoginResponse> responseObserver) {
        try {
            log.info("Received UpdateUserLastLogin request for user ID: {}", request.getUserId());
            UpdateUserLastLoginResponse response = userService.updateUserLastLogin(request);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (NotFoundException e) {
            log.error("User not found with ID: {}", request.getUserId(), e);
            responseObserver.onError(io.grpc.Status.NOT_FOUND
                                         .withDescription(
                                             "User not found with ID: " + request.getUserId())
                                         .asException());
        } catch (Exception e) {
            log.error("Error updating last login time for user ID: {}", request.getUserId(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                                         .withDescription(
                                             "Internal server error: " + e.getMessage())
                                         .asException());
        }
    }

    @Override
    public void createUser(CreateUserRequest request,
                           StreamObserver<UserProfileResponse> responseObserver) {
        try {
            log.info("Received CreateUser request for username: {}", request.getUsername());
            UserProfileResponse response = userService.createUser(request);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            log.error("Invalid create user request: {}", e.getMessage(), e);
            responseObserver.onError(io.grpc.Status.INVALID_ARGUMENT
                                         .withDescription(e.getMessage())
                                         .asException());
        } catch (Exception e) {
            log.error("Error creating user: {}", e.getMessage(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                                         .withDescription(
                                             "Internal server error: " + e.getMessage())
                                         .asException());
        }
    }

    @Override
    public void healthCheck(HealthCheckRequest request,
                            StreamObserver<HealthCheckResponse> responseObserver) {
        log.info("Received HealthCheck request");
        HealthCheckResponse.Builder responseBuilder = HealthCheckResponse.newBuilder()
            .setSuccess(true)
            .setMessage("Users service is healthy");

        HealthCheckResponse.HealthData.Builder healthDataBuilder =
            HealthCheckResponse.HealthData.newBuilder()
                .setTimestamp(Instant.now().toString())
                .setService("users-service")
                .setVersion("1.0.0");

        responseBuilder.setData(healthDataBuilder);
        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getUsersCount(GetUsersCountRequest request,
                              StreamObserver<GetUsersCountResponse> responseObserver) {
        try {
            log.info("Received GetUsersCount request");
            GetUsersCountResponse response = userService.getUsersCount(request);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error getting users count", e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                                        .withDescription(
                                            "Internal server error: " + e.getMessage())
                                        .asException());
        }
    }

    @Override
    public void checkUsernameExists(FindUserByUsernameRequest request,
                                    StreamObserver<ExistsResponse> responseObserver) {
        try {
            log.info("Received CheckUsernameExists request for username: {}",
                     request.getUsername());
            boolean exists = userService.usernameExists(request.getUsername());
            log.info("Username '{}' exists: {}", request.getUsername(), exists);

            // Create and return a proper ExistsResponse now that the stubs are regenerated
            ExistsResponse response = ExistsResponse.newBuilder()
                .setExists(exists)
                .setMessage(exists ? "Username already exists" : "Username is available")
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error in checkUsernameExists", e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                                         .withDescription(
                                             "Internal error checking username existence")
                                         .asException());
        }
    }

    @Override
    public void checkEmailExists(FindUserByEmailRequest request,
                                 StreamObserver<ExistsResponse> responseObserver) {
        try {
            log.info("Received CheckEmailExists request for email: {}", request.getEmail());
            boolean exists = userService.emailExists(request.getEmail());
            log.info("Email '{}' exists: {}", request.getEmail(), exists);

            // Create and return a proper ExistsResponse now that the stubs are regenerated
            ExistsResponse response = ExistsResponse.newBuilder()
                .setExists(exists)
                .setMessage(exists ? "Email already exists" : "Email is available")
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error in checkEmailExists", e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                                         .withDescription("Internal error checking email existence")
                                         .asException());
        }
    }

    @Override
    public void deleteUser(DeleteUserRequest request,
                           StreamObserver<DeleteUserResponse> responseObserver) {
        try {
            log.info("Received DeleteUser request for user ID: {}", request.getUserId());
            boolean success = userService.deleteUser(request.getUserId());
            
            DeleteUserResponse response = DeleteUserResponse.newBuilder()
                .setSuccess(success)
                .setMessage(success ? "User deleted successfully" : "Failed to delete user")
                .build();
                
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (NotFoundException e) {
            log.error("User not found with ID: {}", request.getUserId(), e);
            responseObserver.onError(io.grpc.Status.NOT_FOUND
                                         .withDescription("User not found with ID: " + request.getUserId())
                                         .asException());
        } catch (Exception e) {
            log.error("Error deleting user with ID: {}", request.getUserId(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                                         .withDescription("Internal server error: " + e.getMessage())
                                         .asException());
        }
    }

    @Override
    public void incrementLabsSolved(IncrementLabsSolvedRequest request,
                                   StreamObserver<OperationResponse> responseObserver) {
        try {
            log.info("Received IncrementLabsSolved request for user ID: {}", request.getUserId());
            OperationResponse response = userService.incrementLabsSolved(request);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error incrementing labs solved for user ID: {}", request.getUserId(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                                         .withDescription("Internal server error: " + e.getMessage())
                                         .asException());
        }
    }

    @Override
    public void incrementLabsReviewed(IncrementLabsReviewedRequest request,
                                     StreamObserver<OperationResponse> responseObserver) {
        try {
            log.info("Received IncrementLabsReviewed request for user ID: {}", request.getUserId());
            OperationResponse response = userService.incrementLabsReviewed(request);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error incrementing labs reviewed for user ID: {}", request.getUserId(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                                         .withDescription("Internal server error: " + e.getMessage())
                                         .asException());
        }
    }
}
