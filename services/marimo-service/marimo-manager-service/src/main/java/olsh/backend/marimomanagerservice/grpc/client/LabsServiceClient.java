package olsh.backend.marimomanagerservice.grpc.client;

import olsh.backend.grpc.labs.GetLabRequest;
import olsh.backend.grpc.labs.LabServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class LabsServiceClient {

    private final ManagedChannel channel;
    private final LabServiceGrpc.LabServiceBlockingStub blockingStub;

    public LabsServiceClient(
        @Value("${LABS_SERVICE_HOST:labs-service}") String host,
        @Value("${LABS_SERVICE_PORT:9091}") int port) {
        this.channel = ManagedChannelBuilder.forAddress(host, port)
            .usePlaintext()
            .build();

        this.blockingStub = LabServiceGrpc.newBlockingStub(channel);
        log.info("Initialized LabsServiceClient with connection to {}:{}", host, port);
    }

    public boolean labExists(long labId) {
        if (labId <= 0) {
            return false;
        }
        log.debug("Checking existence of lab with ID: {}", labId);
        try {
            blockingStub.withDeadlineAfter(5, TimeUnit.SECONDS)
                .getLab(GetLabRequest.newBuilder().setLabId(labId).build());
            return true;
        } catch (Exception e) {
            log.warn("Lab with ID {} not found or labs-service is down: {}", labId, e.getMessage());
            return false;
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down LabsServiceClient");
        try {
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.warn("Error shutting down LabsServiceClient", e);
            Thread.currentThread().interrupt();
        }
    }
} 