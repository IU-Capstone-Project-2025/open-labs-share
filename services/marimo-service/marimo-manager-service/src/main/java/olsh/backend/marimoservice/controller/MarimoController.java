package olsh.backend.marimoservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import olsh.backend.marimoservice.annotation.RequireAuth;
import olsh.backend.marimoservice.entity.Component;
import olsh.backend.marimoservice.model.dto.ComponentDto;
import olsh.backend.marimoservice.model.dto.CreateComponentRequestDto;
import olsh.backend.marimoservice.service.ComponentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import olsh.backend.marimoservice.model.dto.UpdateComponentRequestDto;
import olsh.backend.marimoservice.model.dto.PagedResponse;
import olsh.backend.marimoservice.model.dto.StartSessionRequestDto;
import olsh.backend.marimoservice.model.dto.SessionInfoDto;
import olsh.backend.marimoservice.model.dto.ExecuteCellRequestDto;
import olsh.backend.marimoservice.model.dto.ExecuteCellResponseDto;
import olsh.backend.marimoservice.entity.ComponentSession;
import olsh.backend.marimoservice.service.SessionService;
import olsh.backend.marimoservice.service.ExecutionService;
import olsh.backend.marimoservice.entity.ExecutionRecord;
import olsh.backend.marimoservice.model.dto.CellOutputDto;
import olsh.backend.marimoservice.service.AssetService;
import olsh.backend.marimoservice.entity.ComponentAsset;
import olsh.backend.marimoservice.model.dto.AssetInfoDto;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import olsh.backend.marimoservice.exception.OperationFailedException;
import olsh.backend.marimoservice.model.dto.ExecutionRecordDto;
import olsh.backend.marimoservice.model.dto.VariableInfoDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/marimo")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Marimo Controller", description = "Endpoints for managing Marimo components, sessions, and assets")
public class MarimoController {

    private final ComponentService componentService;
    private final SessionService sessionService;
    private final ExecutionService executionService;
    private final AssetService assetService;

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }

    @PostMapping("/components")
    @RequireAuth
    public ResponseEntity<ComponentDto> createComponent(@RequestBody CreateComponentRequestDto request) {
        log.info("Creating component: name={}", request.getName());
        Component component = componentService.createComponent(
                request.getName(),
                request.getContentType(),
                request.getContentId(),
                request.getOwnerId(),
                request.getInitialCode()
        );
        return ResponseEntity.ok(toDto(component));
    }

    @GetMapping("/components/{id}")
    @RequireAuth
    public ResponseEntity<ComponentDto> getComponent(@PathVariable String id) {
        log.debug("Getting component: id={}", id);
        Component component = componentService.getComponent(id);
        return ResponseEntity.ok(toDto(component));
    }

    @PutMapping("/components/{id}")
    @RequireAuth
    public ResponseEntity<ComponentDto> updateComponent(@PathVariable String id, @RequestBody UpdateComponentRequestDto request) {
        log.debug("Updating component: id={}", id);
        Component updatedComponent = componentService.updateComponent(id, request.getName());
        return ResponseEntity.ok(toDto(updatedComponent));
    }

    @DeleteMapping("/components/{id}")
    @RequireAuth
    public ResponseEntity<Void> deleteComponent(@PathVariable String id) {
        log.debug("Deleting component: id={}", id);
        componentService.deleteComponent(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/components")
    @RequireAuth
    public ResponseEntity<PagedResponse<ComponentDto>> listComponents(
            @RequestParam(required = false) String contentType,
            @RequestParam(required = false) String contentId,
            @RequestParam(required = false) String ownerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<Component> componentsPage = componentService.getComponents(contentType, contentId, ownerId, pageRequest);
        return ResponseEntity.ok(toPagedResponse(componentsPage));
    }

    @GetMapping("/components/search")
    @RequireAuth
    public ResponseEntity<PagedResponse<ComponentDto>> searchComponents(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<Component> componentsPage = componentService.searchComponents(query, pageRequest);
        return ResponseEntity.ok(toPagedResponse(componentsPage));
    }

    @PostMapping("/sessions")
    @RequireAuth
    public ResponseEntity<SessionInfoDto> startSession(@RequestBody StartSessionRequestDto request) {
        log.debug("Starting session: componentId={}, userId={}", request.getComponentId(), request.getUserId());
        ComponentSession session = sessionService.startSession(request.getComponentId(), request.getUserId(), request.getSessionName());
        return ResponseEntity.ok(toDto(session));
    }

    @DeleteMapping("/sessions/{id}")
    @RequireAuth
    public ResponseEntity<Void> endSession(@PathVariable String id) {
        log.debug("Ending session: sessionId={}", id);
        sessionService.endSession(id);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/sessions/{id}")
    @RequireAuth
    public ResponseEntity<SessionInfoDto> getSessionStatus(@PathVariable String id) {
        log.debug("Getting status for session: {}", id);
        ComponentSession session = sessionService.getSession(id);
        return ResponseEntity.ok(toDto(session));
    }

    @GetMapping("/sessions")
    @RequireAuth
    public ResponseEntity<PagedResponse<SessionInfoDto>> listUserSessions(
            @RequestParam String userId,
            @RequestParam(defaultValue = "ACTIVE") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        ComponentSession.SessionStatus sessionStatus = ComponentSession.SessionStatus.valueOf(status.toUpperCase());
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<ComponentSession> sessionsPage = sessionService.listUserSessions(userId, sessionStatus, pageRequest);
        
        return ResponseEntity.ok(toSessionPagedResponse(sessionsPage));
    }

    @GetMapping("/sessions/{sessionId}/history")
    @RequireAuth
    public ResponseEntity<PagedResponse<ExecutionRecordDto>> getExecutionHistory(
            @PathVariable String sessionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<ExecutionRecord> historyPage = executionService.getExecutionHistory(sessionId, pageRequest);
        
        return ResponseEntity.ok(toExecutionRecordPagedResponse(historyPage));
    }

    @GetMapping("/sessions/{sessionId}/variables")
    @Operation(summary = "Get session variables")
    @RequireAuth
    public ResponseEntity<List<VariableInfoDto>> getSessionVariables(@PathVariable String sessionId) {
        List<VariableInfoDto> variables = executionService.getSessionVariables(sessionId);
        return ResponseEntity.ok(variables);
    }

    @PostMapping("/sessions/{sessionId}/execute")
    @RequireAuth
    public ResponseEntity<ExecuteCellResponseDto> executeCell(
            @PathVariable String sessionId,
            @RequestBody ExecuteCellRequestDto request) {
        log.debug("Executing cell: sessionId={}, cellId={}", sessionId, request.getCellId());
        ExecuteCellResponseDto response = executionService.executeCell(
                sessionId,
                request.getCellId(),
                request.getCode()
        );
        return ResponseEntity.ok(response);
    }
    
    @PostMapping(value = "/assets/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequireAuth
    public ResponseEntity<AssetInfoDto> uploadAsset(
            @RequestParam("file") MultipartFile file,
            @RequestParam("componentId") String componentId,
            @RequestParam("assetType") String assetType,
            @RequestParam(required = false) Map<String, String> metadata) {
        
        try {
            String assetId = assetService.uploadAsset(
                componentId,
                ComponentAsset.AssetType.valueOf(assetType.toUpperCase()),
                file.getOriginalFilename(),
                file.getBytes(),
                file.getContentType(),
                metadata
            );
            ComponentAsset asset = assetService.getAsset(assetId);
            return ResponseEntity.ok(toDto(asset));
        } catch (Exception e) {
            throw new OperationFailedException("Failed to upload asset.", e);
        }
    }

    @GetMapping("/assets/{id}")
    @RequireAuth
    public ResponseEntity<AssetInfoDto> getAssetInfo(@PathVariable String id) {
        ComponentAsset asset = assetService.getAsset(id);
        return ResponseEntity.ok(toDto(asset));
    }

    @GetMapping("/assets/{id}/download")
    @RequireAuth
    public ResponseEntity<ByteArrayResource> downloadAsset(@PathVariable String id) {
        ComponentAsset asset = assetService.getAsset(id);
        byte[] data = assetService.getAssetData(id);
        ByteArrayResource resource = new ByteArrayResource(data);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + asset.getFileName())
                .contentType(MediaType.parseMediaType(asset.getMimeType()))
                .contentLength(data.length)
                .body(resource);
    }

    @GetMapping("/components/{componentId}/assets")
    @RequireAuth
    public ResponseEntity<List<AssetInfoDto>> listAssets(@PathVariable String componentId) {
        List<ComponentAsset> assets = assetService.getAssetsByComponent(componentId);
        List<AssetInfoDto> dtos = assets.stream().map(this::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @DeleteMapping("/assets/{id}")
    @RequireAuth
    public ResponseEntity<Void> deleteAsset(@PathVariable String id) {
        assetService.deleteAsset(id);
        return ResponseEntity.noContent().build();
    }
    
    private SessionInfoDto toDto(ComponentSession session) {
        // Fetch dynamic counts
        int variableCount = sessionService.getVariableCount(session.getId());
        long executionCount = sessionService.getExecutionCount(session.getId());

        return new SessionInfoDto(
                session.getId(),
                session.getComponent().getId(),
                session.getUserId(),
                session.getSessionName(),
                session.getStatus().toString(),
                session.getCreatedAt(),
                session.getLastAccessed(),
                session.getExpiresAt(),
                variableCount,
                (int) executionCount
        );
    }

    private ComponentDto toDto(Component component) {
        return new ComponentDto(
                component.getId(),
                component.getName(),
                component.getContentType(),
                component.getContentId(),
                component.getOwnerId(),
                component.getNotebookPath(),
                component.getCreatedAt(),
                component.getUpdatedAt()
        );
    }

    private PagedResponse<ComponentDto> toPagedResponse(Page<Component> page) {
        return new PagedResponse<>(
                page.getContent().stream().map(this::toDto).collect(Collectors.toList()),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

    private PagedResponse<SessionInfoDto> toSessionPagedResponse(Page<ComponentSession> page) {
        return new PagedResponse<>(
                page.getContent().stream().map(this::toDto).collect(Collectors.toList()),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

    private AssetInfoDto toDto(ComponentAsset asset) {
        String downloadUrl = "/api/v1/marimo/assets/" + asset.getId() + "/download";
        return new AssetInfoDto(
            asset.getId(),
            asset.getComponent().getId(),
            asset.getAssetType().toString(),
            asset.getFileName(),
            asset.getFilePath(),
            asset.getMimeType(),
            asset.getFileSize(),
            asset.getCreatedAt(),
            asset.getMetadata(),
            downloadUrl,
            null // Thumbnail URL not implemented yet
        );
    }

    private PagedResponse<ExecutionRecordDto> toExecutionRecordPagedResponse(Page<ExecutionRecord> page) {
        return new PagedResponse<>(
                page.getContent().stream().map(this::toDto).collect(Collectors.toList()),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

    private ExecutionRecordDto toDto(ExecutionRecord record) {
        return new ExecutionRecordDto(
                record.getId(),
                record.getSession().getId(),
                record.getCellId(),
                record.getCode(),
                record.isSuccess(),
                record.getErrorMessage(),
                record.getExecutionTimeMs(),
                record.getCreatedAt(),
                record.getOutputCount()
        );
    }
} 