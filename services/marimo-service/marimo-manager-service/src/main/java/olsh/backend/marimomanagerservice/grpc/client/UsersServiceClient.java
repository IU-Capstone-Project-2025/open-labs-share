package olsh.backend.marimomanagerservice.grpc.client;

import com.olsh.users.proto.GetUserProfileRequest;
import com.olsh.users.proto.UsersServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class UsersServiceClient {

    private final ManagedChannel channel;
    private final UsersServiceGrpc.UsersServiceBlockingStub blockingStub;

    public UsersServiceClient(
        @Value("${USERS_SERVICE_HOST:users-service}") String host,
        @Value("${USERS_SERVICE_PORT:9093}") int port) {
        this.channel = ManagedChannelBuilder.forAddress(host, port)
            .usePlaintext()
            .build();

        this.blockingStub = UsersServiceGrpc.newBlockingStub(channel);
        log.info("Initialized UsersServiceClient with connection to {}:{}", host, port);
    }

    public boolean userExists(long userId) {
        if (userId <= 0) {
            return false;
        }
        log.debug("Checking existence of user with ID: {}", userId);
        try {
            blockingStub.withDeadlineAfter(5, TimeUnit.SECONDS)
                .getUserProfile(GetUserProfileRequest.newBuilder().setUserId(userId).build());
            return true;
        } catch (Exception e) {
            log.warn("User with ID {} not found or users-service is down: {}", userId, e.getMessage());
            return false;
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down UsersServiceClient");
        try {
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.warn("Error shutting down UsersServiceClient", e);
            Thread.currentThread().interrupt();
        }
    }
} 