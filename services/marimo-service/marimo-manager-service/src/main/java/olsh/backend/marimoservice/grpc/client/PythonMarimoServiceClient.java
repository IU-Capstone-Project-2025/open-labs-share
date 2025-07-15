package olsh.backend.marimoservice.grpc.client;

import io.grpc.ManagedChannel;
import lombok.extern.slf4j.Slf4j;
import olsh.backend.marimoservice.grpc.proto.MarimoExecutorGrpc;
import olsh.backend.marimoservice.grpc.proto.PythonMarimoServiceProto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PythonMarimoServiceClient {

    private final MarimoExecutorGrpc.MarimoExecutorBlockingStub blockingStub;

    public PythonMarimoServiceClient(@Qualifier("pythonMarimoServiceChannel") ManagedChannel pythonMarimoChannel) {
        this.blockingStub = MarimoExecutorGrpc.newBlockingStub(pythonMarimoChannel);
    }

    public PythonMarimoServiceProto.StartSessionResponse startSession(String sessionId, String notebookPath) {
        log.info("Sending start session request to Python service for sessionId: {}", sessionId);
        PythonMarimoServiceProto.StartSessionRequest request = PythonMarimoServiceProto.StartSessionRequest.newBuilder()
                .setSessionId(sessionId)
                .setNotebookPath(notebookPath)
                .build();
        try {
            return blockingStub.startSession(request);
        } catch (Exception e) {
            log.error("Error calling Python service startSession", e);
            throw new RuntimeException("Failed to start session in Python service", e);
        }
    }

    public PythonMarimoServiceProto.ExecuteResponse executeCell(String sessionId, String cellId, String code) {
        log.debug("Sending execute cell request to Python service for sessionId: {}", sessionId);
        PythonMarimoServiceProto.ExecuteRequest request = PythonMarimoServiceProto.ExecuteRequest.newBuilder()
                .setSessionId(sessionId)
                .setCellId(cellId)
                .setCode(code)
                .build();
        try {
            return blockingStub.executeCell(request);
        } catch (Exception e) {
            log.error("Error calling Python service executeCell for session {}", sessionId, e);
            throw new RuntimeException("Failed to execute code in Python service", e);
        }
    }

    public PythonMarimoServiceProto.EndSessionResponse endSession(String sessionId) {
        log.info("Sending end session request to Python service for sessionId: {}", sessionId);
        PythonMarimoServiceProto.EndSessionRequest request = PythonMarimoServiceProto.EndSessionRequest.newBuilder()
                .setSessionId(sessionId)
                .build();
        try {
            return blockingStub.endSession(request);
        } catch (Exception e) {
            log.error("Error calling Python service endSession", e);
            return PythonMarimoServiceProto.EndSessionResponse.newBuilder().setSuccess(false).setError(e.getMessage()).build();
        }
    }

    public PythonMarimoServiceProto.SessionStateResponse getSessionState(String sessionId) {
        log.debug("Requesting session state from Python service for sessionId: {}", sessionId);
        PythonMarimoServiceProto.SessionStateRequest request = PythonMarimoServiceProto.SessionStateRequest.newBuilder()
                .setSessionId(sessionId)
                .build();
        try {
            return blockingStub.getSessionState(request);
        } catch (Exception e) {
            log.error("Error calling Python service getSessionState for session {}", sessionId, e);
            throw new RuntimeException("Failed to get session state from Python service", e);
        }
    }
} 