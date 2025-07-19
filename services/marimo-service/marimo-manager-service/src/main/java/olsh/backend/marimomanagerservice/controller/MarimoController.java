package olsh.backend.marimomanagerservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import olsh.backend.marimomanagerservice.model.annotation.RequireAuth;
import olsh.backend.marimomanagerservice.entity.Component;
import olsh.backend.marimomanagerservice.entity.ComponentAsset;
import olsh.backend.marimomanagerservice.entity.ComponentSession;
import olsh.backend.marimomanagerservice.entity.ExecutionRecord;
import olsh.backend.marimomanagerservice.exception.OperationFailedException;
import olsh.backend.marimomanagerservice.model.dto.*;
import olsh.backend.marimomanagerservice.service.AssetService;
import olsh.backend.marimomanagerservice.service.ComponentService;
import olsh.backend.marimomanagerservice.service.ExecutionService;
import olsh.backend.marimomanagerservice.service.SessionService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import olsh.backend.marimomanagerservice.model.dto.UserInfo;
import org.springframework.http.HttpStatus;

import java.util.Collections;
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
    public ResponseEntity<ComponentDto> createComponent(
            @RequestBody CreateComponentRequestDto request,
            HttpServletRequest httpRequest) {

        UserInfo userInfo = (UserInfo) httpRequest.getAttribute("authenticatedUser");
        if (userInfo == null) {
            log.warn("User info not found in request attributes, which should have been set by AuthInterceptor.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String ownerId = String.valueOf(userInfo.getId());


        log.info("Creating component: name={}", request.getName());
        Component component = componentService.createComponent(
                request.getName(),
                request.getContentType(),
                request.getContentId(),
                ownerId,
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

    @GetMapping("/components/{contentType}/{contentId}")
    @RequireAuth
    public ResponseEntity<List<ComponentDto>> getComponentsByContent(
            @PathVariable String contentType,
            @PathVariable String contentId) {
        log.debug("Getting components for content: contentType={}, contentId={}", contentType, contentId);
        List<Component> components = componentService.getComponentsByContent(contentType, contentId);
        if (components == null || components.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        return ResponseEntity.ok(components.stream().map(this::toDto).collect(Collectors.toList()));
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

    @DeleteMapping("/components/by-content")
    @Operation(summary = "Delete all marimo components for a specific content (lab or article)")
    public ResponseEntity<String> deleteComponentsForContent(
            @RequestParam String contentType,
            @RequestParam String contentId) {
        log.info("Deleting all marimo components for contentType={}, contentId={}", contentType, contentId);
        
        try {
            componentService.deleteComponentsForContent(contentType, contentId);
            return ResponseEntity.ok("Successfully deleted all marimo components for " + contentType + " " + contentId);
        } catch (Exception e) {
            log.error("Failed to delete components for content: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body("Failed to delete marimo components: " + e.getMessage());
        }
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
    public ResponseEntity<SessionInfoDto> startSession(
            @RequestBody StartSessionRequestDto request,
            HttpServletRequest httpRequest) {
        UserInfo userInfo = (UserInfo) httpRequest.getAttribute("authenticatedUser");
        if (userInfo == null) {
            log.warn("User info not found in request attributes, which should have been set by AuthInterceptor.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.debug("Starting session: componentId={}, userId={}", request.getComponentId(), userInfo.getId());
        ComponentSession session = sessionService.startSession(
                request.getComponentId(),
                String.valueOf(userInfo.getId()),
                request.getSessionName()
        );
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
            @RequestParam(defaultValue = "ACTIVE") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest httpRequest) {

        UserInfo userInfo = (UserInfo) httpRequest.getAttribute("authenticatedUser");
        if (userInfo == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        ComponentSession.SessionStatus sessionStatus = ComponentSession.SessionStatus.valueOf(status.toUpperCase());
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<ComponentSession> sessionsPage = sessionService.listUserSessions(String.valueOf(userInfo.getId()), sessionStatus, pageRequest);
        
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

    @PutMapping("/sessions/{sessionId}/widgets/{widgetId}/value")
    @RequireAuth
    public ResponseEntity<Void> updateWidgetValue(
            @PathVariable String sessionId,
            @PathVariable String widgetId,
            @RequestBody UpdateWidgetValueRequestDto request) {
        log.debug("Updating widget value: sessionId={}, widgetId={}, value={}", sessionId, widgetId, request.getValue());
        executionService.updateWidgetValue(sessionId, widgetId, request.getValue());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/sessions/{sessionId}/widgets/batch")
    @RequireAuth
    public ResponseEntity<BatchUpdateResponseDto> batchUpdateWidgets(
            @PathVariable String sessionId,
            @RequestBody BatchUpdateWidgetsRequestDto request) {
        log.debug("Batch updating widgets: sessionId={}, count={}", sessionId, request.getUpdates().size());
        BatchUpdateResponseDto response = executionService.batchUpdateWidgets(sessionId, request.getUpdates());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sessions/{sessionId}/widgets/analytics")
    @RequireAuth
    public ResponseEntity<WidgetAnalyticsDto> getWidgetAnalytics(@PathVariable String sessionId) {
        log.debug("Getting widget analytics: sessionId={}", sessionId);
        WidgetAnalyticsDto analytics = executionService.getWidgetAnalytics(sessionId);
        return ResponseEntity.ok(analytics);
    }

    @GetMapping("/sessions/{sessionId}/widgets/{widgetId}/state")
    @RequireAuth
    public ResponseEntity<WidgetStateDto> getWidgetState(
            @PathVariable String sessionId,
            @PathVariable String widgetId) {
        log.debug("Getting widget state: sessionId={}, widgetId={}", sessionId, widgetId);
        WidgetStateDto state = executionService.getWidgetState(sessionId, widgetId);
        return ResponseEntity.ok(state);
    }

    @GetMapping("/sessions/{sessionId}/widgets/{widgetId}/constraints")
    @RequireAuth
    public ResponseEntity<WidgetConstraintsDto> getWidgetConstraints(
            @PathVariable String sessionId,
            @PathVariable String widgetId) {
        log.debug("Getting widget constraints: sessionId={}, widgetId={}", sessionId, widgetId);
        WidgetConstraintsDto constraints = executionService.getWidgetConstraints(sessionId, widgetId);
        return ResponseEntity.ok(constraints);
    }

    @PostMapping("/sessions/{sessionId}/widgets/{widgetId}/state")
    @RequireAuth
    public ResponseEntity<Void> saveWidgetState(
            @PathVariable String sessionId,
            @PathVariable String widgetId,
            @RequestBody Map<String, Object> state) {
        log.debug("Saving widget state: sessionId={}, widgetId={}", sessionId, widgetId);
        executionService.saveWidgetState(sessionId, widgetId, state);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/sessions/{sessionId}/widgets/{widgetId}/state/raw")
    @RequireAuth
    public ResponseEntity<Map<String, Object>> loadWidgetState(
            @PathVariable String sessionId,
            @PathVariable String widgetId) {
        log.debug("Loading widget state: sessionId={}, widgetId={}", sessionId, widgetId);
        Map<String, Object> state = executionService.loadWidgetState(sessionId, widgetId);
        return ResponseEntity.ok(state);
    }

    @DeleteMapping("/sessions/{sessionId}/widgets/{widgetId}/state")
    @RequireAuth
    public ResponseEntity<Void> deleteWidgetState(
            @PathVariable String sessionId,
            @PathVariable String widgetId) {
        log.debug("Deleting widget state: sessionId={}, widgetId={}", sessionId, widgetId);
        executionService.deleteWidgetState(sessionId, widgetId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/templates")
    @RequireAuth
    public ResponseEntity<List<WidgetTemplateDto>> getWidgetTemplates(
            @RequestParam(required = false) String category) {
        log.debug("Getting widget templates: category={}", category);
        List<WidgetTemplateDto> templates = executionService.getWidgetTemplates(category);
        return ResponseEntity.ok(templates);
    }

    @PostMapping("/templates")
    @RequireAuth
    public ResponseEntity<WidgetTemplateDto> saveWidgetTemplate(
            @RequestBody WidgetTemplateDto template) {
        log.debug("Saving widget template: {}", template.getId());
        WidgetTemplateDto savedTemplate = executionService.saveWidgetTemplate(template);
        return ResponseEntity.ok(savedTemplate);
    }

    @DeleteMapping("/templates/{templateId}")
    @RequireAuth
    public ResponseEntity<Void> deleteWidgetTemplate(
            @PathVariable String templateId) {
        log.debug("Deleting widget template: {}", templateId);
        executionService.deleteWidgetTemplate(templateId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/sessions/{sessionId}/widgets/{widgetId}/versions")
    @RequireAuth
    public ResponseEntity<List<WidgetVersionDto>> getWidgetVersions(
            @PathVariable String sessionId,
            @PathVariable String widgetId) {
        log.debug("Getting widget versions: sessionId={}, widgetId={}", sessionId, widgetId);
        List<WidgetVersionDto> versions = executionService.getWidgetVersions(sessionId, widgetId);
        return ResponseEntity.ok(versions);
    }

    @PostMapping("/sessions/{sessionId}/widgets/{widgetId}/versions")
    @RequireAuth
    public ResponseEntity<WidgetVersionDto> createWidgetVersion(
            @PathVariable String sessionId,
            @PathVariable String widgetId,
            @RequestBody Map<String, Object> version) {
        log.debug("Creating widget version: sessionId={}, widgetId={}", sessionId, widgetId);
        WidgetVersionDto versionDto = executionService.createWidgetVersion(sessionId, widgetId, version);
        return ResponseEntity.ok(versionDto);
    }

    @PostMapping("/sessions/{sessionId}/widgets/{widgetId}/versions/{versionId}/revert")
    @RequireAuth
    public ResponseEntity<Void> revertWidgetToVersion(
            @PathVariable String sessionId,
            @PathVariable String widgetId,
            @PathVariable int versionId) {
        log.debug("Reverting widget to version: sessionId={}, widgetId={}, versionId={}", sessionId, widgetId, versionId);
        executionService.revertWidgetToVersion(sessionId, widgetId, versionId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/sessions/{sessionId}/collaborators")
    @RequireAuth
    public ResponseEntity<WidgetCollaborationDto> getSessionCollaborators(
            @PathVariable String sessionId) {
        log.debug("Getting session collaborators: sessionId={}", sessionId);
        WidgetCollaborationDto collaboration = executionService.getSessionCollaborators(sessionId);
        return ResponseEntity.ok(collaboration);
    }

    @PostMapping("/sessions/{sessionId}/widgets/{widgetId}/lock")
    @RequireAuth
    public ResponseEntity<Void> requestWidgetLock(
            @PathVariable String sessionId,
            @PathVariable String widgetId,
            @RequestParam String userId) {
        log.debug("Requesting widget lock: sessionId={}, widgetId={}, userId={}", sessionId, widgetId, userId);
        executionService.requestWidgetLock(sessionId, widgetId, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/sessions/{sessionId}/widgets/{widgetId}/lock")
    @RequireAuth
    public ResponseEntity<Void> releaseWidgetLock(
            @PathVariable String sessionId,
            @PathVariable String widgetId,
            @RequestParam String userId) {
        log.debug("Releasing widget lock: sessionId={}, widgetId={}, userId={}", sessionId, widgetId, userId);
        executionService.releaseWidgetLock(sessionId, widgetId, userId);
        return ResponseEntity.ok().build();
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