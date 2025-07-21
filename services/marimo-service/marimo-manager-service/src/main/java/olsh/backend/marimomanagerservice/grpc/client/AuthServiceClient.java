package olsh.backend.marimomanagerservice.grpc.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import olsh.backend.grpc.auth.AuthServiceGrpc;
import olsh.backend.grpc.auth.ValidateTokenRequest;
import olsh.backend.grpc.auth.ValidateTokenResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class AuthServiceClient {

    private final ManagedChannel channel;
    private final AuthServiceGrpc.AuthServiceBlockingStub blockingStub;

    public AuthServiceClient(
        @Value("${AUTH_SERVICE_HOST:auth-service}") String host,
        @Value("${AUTH_SERVICE_PORT:9092}") int port) {
        this.channel = ManagedChannelBuilder.forAddress(host, port)
            .usePlaintext()
            .build();

        this.blockingStub = AuthServiceGrpc.newBlockingStub(channel);
        log.info("Initialized AuthServiceClient with connection to {}:{}", host, port);
    }

    public ValidateTokenResponse validateToken(String token) {
        if (token == null || token.isBlank()) {
            return ValidateTokenResponse.newBuilder().setValid(false).build();
        }
        try {
            log.debug("Validating token with auth-service");
            ValidateTokenRequest request = ValidateTokenRequest.newBuilder().setToken(token).build();
            return blockingStub.withDeadlineAfter(5, TimeUnit.SECONDS).validateToken(request);
        } catch (Exception e) {
            log.error("gRPC error calling auth-service", e);
            return ValidateTokenResponse.newBuilder().setValid(false).setErrorMessage(e.getMessage()).build();
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down AuthServiceClient");
        try {
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.warn("Error shutting down AuthServiceClient", e);
            Thread.currentThread().interrupt();
        }
    }
} 