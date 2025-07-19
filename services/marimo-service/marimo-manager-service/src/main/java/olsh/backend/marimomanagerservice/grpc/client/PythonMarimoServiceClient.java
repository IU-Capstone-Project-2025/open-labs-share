package olsh.backend.marimomanagerservice.grpc.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import olsh.backend.grpc.marimo.MarimoExecutorGrpc;
import olsh.backend.grpc.marimo.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class PythonMarimoServiceClient {

    private final ManagedChannel channel;
    private final MarimoExecutorGrpc.MarimoExecutorBlockingStub blockingStub;

    public PythonMarimoServiceClient(
        @Value("${PYTHON_SERVICE_HOST:marimo-executor-service}") String host,
        @Value("${PYTHON_SERVICE_PORT:9095}") int port) {
        this.channel = ManagedChannelBuilder.forAddress(host, port)
            .usePlaintext()
            .build();

        this.blockingStub = MarimoExecutorGrpc.newBlockingStub(channel);
        log.info("Initialized PythonMarimoServiceClient with connection to {}:{}", host, port);
    }

    public StartSessionResponse startSession(String sessionId, String notebookPath, String componentId) {
        log.info("Sending start session request to Python service for sessionId: {}, componentId: {}", sessionId, componentId);
        StartSessionRequest.Builder requestBuilder = StartSessionRequest.newBuilder()
                .setSessionId(sessionId)
                .setNotebookPath(notebookPath);
        
        if (componentId != null && !componentId.isEmpty()) {
            requestBuilder.setComponentId(componentId);
        }
        
        StartSessionRequest request = requestBuilder.build();
        
        try {
            return blockingStub.withDeadlineAfter(30, TimeUnit.SECONDS).startSession(request);
        } catch (Exception e) {
            log.error("Error starting session: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to start session", e);
        }
    }

    public ExecuteResponse executeCell(String sessionId, String cellId, String code) {
        log.info("Sending execute cell request to Python service for sessionId: {}, cellId: {}", sessionId, cellId);
        ExecuteRequest request = ExecuteRequest.newBuilder()
                .setSessionId(sessionId)
                .setCellId(cellId)
                .setCode(code)
                .build();
        
        try {
            return blockingStub.withDeadlineAfter(30, TimeUnit.SECONDS).executeCell(request);
        } catch (Exception e) {
            log.error("Error executing cell: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to execute cell", e);
        }
    }

    public EndSessionResponse endSession(String sessionId) {
        log.info("Sending end session request to Python service for sessionId: {}", sessionId);
        EndSessionRequest request = EndSessionRequest.newBuilder()
                .setSessionId(sessionId)
                .build();
        
        try {
            return blockingStub.withDeadlineAfter(10, TimeUnit.SECONDS).endSession(request);
        } catch (Exception e) {
            log.error("Error ending session: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to end session", e);
        }
    }

    public SessionStateResponse getSessionState(String sessionId) {
        log.info("Getting session state from Python service for sessionId: {}", sessionId);
        SessionStateRequest request = SessionStateRequest.newBuilder()
                .setSessionId(sessionId)
                .build();
        
        try {
            return blockingStub.withDeadlineAfter(10, TimeUnit.SECONDS).getSessionState(request);
        } catch (Exception e) {
            log.error("Error getting session state: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get session state", e);
        }
    }

    public void updateWidgetValue(String sessionId, String widgetId, String value) {
        log.info("Updating widget value in Python service for sessionId: {}, widgetId: {}", sessionId, widgetId);
        UpdateWidgetValueRequest request = UpdateWidgetValueRequest.newBuilder()
                .setSessionId(sessionId)
                .setWidgetId(widgetId)
                .setValue(value)
                .build();
        
        try {
            blockingStub.withDeadlineAfter(10, TimeUnit.SECONDS).updateWidgetValue(request);
        } catch (Exception e) {
            log.error("Error updating widget value: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update widget value", e);
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down PythonMarimoServiceClient");
        try {
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.warn("Error shutting down PythonMarimoServiceClient", e);
            Thread.currentThread().interrupt();
        }
    }
} 