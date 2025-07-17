package olsh.backend.marimoservice.grpc.client;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import olsh.backend.marimoservice.config.MarimoProperties;
import olsh.backend.marimoservice.grpc.proto.MarimoExecutorGrpc;
import olsh.backend.marimoservice.grpc.proto.PythonMarimoServiceProto.EndSessionRequest;
import olsh.backend.marimoservice.grpc.proto.PythonMarimoServiceProto.EndSessionResponse;
import olsh.backend.marimoservice.grpc.proto.PythonMarimoServiceProto.ExecuteRequest;
import olsh.backend.marimoservice.grpc.proto.PythonMarimoServiceProto.ExecuteResponse;
import olsh.backend.marimoservice.grpc.proto.PythonMarimoServiceProto.SessionStateRequest;
import olsh.backend.marimoservice.grpc.proto.PythonMarimoServiceProto.SessionStateResponse;
import olsh.backend.marimoservice.grpc.proto.PythonMarimoServiceProto.StartSessionRequest;
import olsh.backend.marimoservice.grpc.proto.PythonMarimoServiceProto.StartSessionResponse;
@Service
@RequiredArgsConstructor
@Slf4j
public class PythonMarimoClient {

    @GrpcClient("python-service")
    private MarimoExecutorGrpc.MarimoExecutorBlockingStub pythonServiceStub;

    private final MarimoProperties marimoProperties;

    @Retryable(
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public StartSessionResponse startSession(String sessionId, String notebookPath) {
        log.debug("Starting Python session: sessionId={}, notebookPath={}", sessionId, notebookPath);
        
        try {
            StartSessionRequest request = StartSessionRequest.newBuilder()
                    .setSessionId(sessionId)
                    .setNotebookPath(notebookPath)
                    .build();

            StartSessionResponse response = pythonServiceStub.startSession(request);
            log.debug("Python session started: sessionId={}, success={}", sessionId, response.getSuccess());
            return response;
        } catch (Exception e) {
            log.error("Failed to start Python session: sessionId={}, error={}", sessionId, e.getMessage(), e);
            throw new RuntimeException("Failed to start Python session", e);
        }
    }

    @Retryable(
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public ExecuteResponse executeCell(String sessionId, String cellId, String code) {
        log.debug("Executing cell: sessionId={}, cellId={}", sessionId, cellId);
        
        try {
            ExecuteRequest request = ExecuteRequest.newBuilder()
                    .setSessionId(sessionId)
                    .setCellId(cellId)
                    .setCode(code)
                    .build();

            ExecuteResponse response = pythonServiceStub.executeCell(request);
            log.debug("Cell executed: sessionId={}, cellId={}, success={}", 
                    sessionId, cellId, response.getSuccess());
            return response;
        } catch (Exception e) {
            log.error("Failed to execute cell: sessionId={}, cellId={}, error={}", 
                    sessionId, cellId, e.getMessage(), e);
            throw new RuntimeException("Failed to execute cell", e);
        }
    }

    @Retryable(
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public EndSessionResponse endSession(String sessionId) {
        log.debug("Ending Python session: sessionId={}", sessionId);
        
        try {
            EndSessionRequest request = EndSessionRequest.newBuilder()
                    .setSessionId(sessionId)
                    .build();

            EndSessionResponse response = pythonServiceStub.endSession(request);
            log.debug("Python session ended: sessionId={}, success={}", sessionId, response.getSuccess());
            return response;
        } catch (Exception e) {
            log.error("Failed to end Python session: sessionId={}, error={}", sessionId, e.getMessage(), e);
            throw new RuntimeException("Failed to end Python session", e);
        }
    }

    @Retryable(
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public SessionStateResponse getSessionState(String sessionId) {
        log.debug("Getting Python session state: sessionId={}", sessionId);
        
        try {
            SessionStateRequest request = SessionStateRequest.newBuilder()
                    .setSessionId(sessionId)
                    .build();

            SessionStateResponse response = pythonServiceStub.getSessionState(request);
            log.debug("Python session state retrieved: sessionId={}, exists={}", 
                    sessionId, response.getExists());
            return response;
        } catch (Exception e) {
            log.error("Failed to get Python session state: sessionId={}, error={}", 
                    sessionId, e.getMessage(), e);
            throw new RuntimeException("Failed to get Python session state", e);
        }
    }
} 