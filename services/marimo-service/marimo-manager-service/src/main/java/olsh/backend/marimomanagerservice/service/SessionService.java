package olsh.backend.marimomanagerservice.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import olsh.backend.marimomanagerservice.entity.Component;
import olsh.backend.marimomanagerservice.entity.ComponentSession;
import olsh.backend.marimomanagerservice.exception.OperationFailedException;
import olsh.backend.marimomanagerservice.exception.ResourceNotFoundException;
import olsh.backend.marimomanagerservice.grpc.client.PythonMarimoServiceClient;
import olsh.backend.marimomanagerservice.repository.ComponentRepository;
import olsh.backend.marimomanagerservice.repository.ComponentSessionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import olsh.backend.marimomanagerservice.repository.ExecutionRecordRepository;


@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SessionService {

    private final ComponentRepository componentRepository;
    private final ComponentSessionRepository sessionRepository;
    private final PythonMarimoServiceClient pythonMarimoServiceClient;
    private final ExecutionRecordRepository executionRecordRepository;

    public ComponentSession startSession(String componentId, String userId, String sessionName) {
        log.debug("Starting session: componentId={}, userId={}, sessionName={}", componentId, userId, sessionName);
        
        Component component = componentRepository.findById(componentId)
                .orElseThrow(() -> new ResourceNotFoundException("Cannot start session. Component not found: " + componentId));

        String sessionId = UUID.randomUUID().toString();

        try {
            pythonMarimoServiceClient.startSession(sessionId, component.getNotebookPath(), componentId);
        } catch (Exception e) {
            log.error("Failed to start Python kernel for component {}: {}", componentId, e.getMessage(), e);
            throw new OperationFailedException("Failed to initialize session in the backend executor.", e);
        }

        ComponentSession session = ComponentSession.builder()
                .id(sessionId)
                .component(component)
                .userId(userId)
                .sessionName(sessionName)
                .status(ComponentSession.SessionStatus.ACTIVE)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();
        
        ComponentSession savedSession = sessionRepository.save(session);
        log.info("Session started successfully: sessionId={}, componentId={}, userId={}", savedSession.getId(), componentId, userId);
        return savedSession;
    }

    public void endSession(String sessionId) {
        log.debug("Ending session: sessionId={}", sessionId);
        
        ComponentSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));

        try {
            pythonMarimoServiceClient.endSession(sessionId);
        } catch (Exception e) {
            log.warn("Python service failed to end session, but manager service will proceed with cleanup: sessionId={}", sessionId);
        }

        try {
            session.setStatus(ComponentSession.SessionStatus.EXPIRED);
            sessionRepository.save(session);
            log.info("Session ended: sessionId={}", sessionId);
        } catch (Exception e) {
            log.error("Failed to update session status to EXPIRED for sessionId {}: {}", sessionId, e.getMessage(), e);
            throw new OperationFailedException("Failed to finalize session ending.", e);
        }
    }
    
    public void validateSession(String sessionId) {
        ComponentSession session = getSession(sessionId);
        if (session.getStatus() != ComponentSession.SessionStatus.ACTIVE) {
            throw new IllegalStateException("Session is not active: " + sessionId);
        }
        if (session.getExpiresAt() != null && session.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Session has expired: " + sessionId);
        }
    }

    public ComponentSession getSession(String sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));
    }

    public Page<ComponentSession> listUserSessions(String userId, ComponentSession.SessionStatus status, Pageable pageable) {
        return sessionRepository.findByUserIdAndStatus(userId, status, pageable);
    }
    
    public int getVariableCount(String sessionId) {
        try {
            return pythonMarimoServiceClient.getSessionState(sessionId).getStateMap().size();
        } catch (Exception e) {
             log.warn("Could not retrieve variable count from python service for session {}: {}", sessionId, e.getMessage());
        }
        return 0;
    }

    public long getExecutionCount(String sessionId) {
        return executionRecordRepository.countBySessionId(sessionId);
    }
} 