package olsh.backend.marimoservice.grpc.client;

import io.grpc.ManagedChannel;
import com.olsh.labs.proto.GetLabRequest;
import com.olsh.labs.proto.LabServiceGrpc;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LabsServiceClient {

    private final LabServiceGrpc.LabServiceBlockingStub blockingStub;

    public LabsServiceClient(@Qualifier("labsServiceChannel") ManagedChannel labsServiceChannel) {
        this.blockingStub = LabServiceGrpc.newBlockingStub(labsServiceChannel);
    }

    public boolean labExists(long labId) {
        if (labId <= 0) {
            return false;
        }
        log.debug("Checking existence of lab with ID: {}", labId);
        try {
            blockingStub.getLab(GetLabRequest.newBuilder().setLabId(labId).build());
            return true;
        } catch (Exception e) {
            log.warn("Lab with ID {} not found or labs-service is down: {}", labId, e.getMessage());
            return false;
        }
    }
} 