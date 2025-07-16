package olsh.backend.marimoservice.grpc.client;

import com.olsh.users.proto.GetUserProfileRequest;
import com.olsh.users.proto.UsersServiceGrpc;
import io.grpc.ManagedChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class UsersServiceClient {

    private final UsersServiceGrpc.UsersServiceBlockingStub blockingStub;

    public UsersServiceClient(@Qualifier("usersServiceChannel") ManagedChannel usersServiceChannel) {
        this.blockingStub = UsersServiceGrpc.newBlockingStub(usersServiceChannel);
    }

    public boolean userExists(long userId) {
        if (userId <= 0) {
            return false;
        }
        log.debug("Checking existence of user with ID: {}", userId);
        try {
            blockingStub.getUserProfile(GetUserProfileRequest.newBuilder().setUserId(userId).build());
            return true;
        } catch (Exception e) {
            log.warn("User with ID {} not found or users-service is down: {}", userId, e.getMessage());
            return false;
        }
    }
} 