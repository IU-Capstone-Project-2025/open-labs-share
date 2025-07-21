package olsh.backend.marimomanagerservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import olsh.backend.marimomanagerservice.grpc.client.PythonMarimoServiceClient;
import olsh.backend.grpc.marimo.*;
import olsh.backend.marimomanagerservice.repository.ExecutionRecordRepository;
import olsh.backend.marimomanagerservice.repository.WidgetStateRepository;
import olsh.backend.marimomanagerservice.entity.WidgetState;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import olsh.backend.marimomanagerservice.entity.ExecutionRecord;
import olsh.backend.marimomanagerservice.model.dto.CellOutputDto;
import olsh.backend.marimomanagerservice.model.dto.ExecuteCellResponseDto;
import olsh.backend.marimomanagerservice.model.dto.VariableInfoDto;
import olsh.backend.marimomanagerservice.model.dto.BatchUpdateWidgetsRequestDto;
import olsh.backend.marimomanagerservice.model.dto.BatchUpdateResponseDto;
import olsh.backend.marimomanagerservice.model.dto.WidgetAnalyticsDto;
import olsh.backend.marimomanagerservice.model.dto.WidgetStateDto;
import olsh.backend.marimomanagerservice.model.dto.WidgetConstraintsDto;
import olsh.backend.marimomanagerservice.model.dto.WidgetTemplateDto;
import olsh.backend.marimomanagerservice.model.dto.WidgetVersionDto;
import olsh.backend.marimomanagerservice.model.dto.WidgetCollaborationDto;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExecutionService {

    private final PythonMarimoServiceClient pythonMarimoServiceClient;
    private final SessionService sessionService;
    private final ExecutionRecordRepository executionRecordRepository;
    private final WidgetStateRepository widgetStateRepository;
    private final ObjectMapper objectMapper;

    public ExecuteCellResponseDto executeCell(String sessionId, String cellId, String code) {
        sessionService.validateSession(sessionId);

        long startTime = System.currentTimeMillis();
        ExecuteResponse pythonResponse = pythonMarimoServiceClient.executeCell(sessionId, cellId, code);
        long executionTime = System.currentTimeMillis() - startTime;

        ExecutionRecord record = ExecutionRecord.builder()
                .session(sessionService.getSession(sessionId))
                .cellId(cellId)
                .code(code)
                .success(pythonResponse.getSuccess())
                .errorMessage(pythonResponse.getError())
                .executionTimeMs(executionTime)
                .outputCount(pythonResponse.getOutputsList().size())
                .build();
        executionRecordRepository.save(record);

        List<CellOutputDto> outputs = pythonResponse.getOutputsList().stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return new ExecuteCellResponseDto(
                record.getId(),
                outputs,
                pythonResponse.getCellStateMap(),
                executionTime
        );
    }

    public Page<ExecutionRecord> getExecutionHistory(String sessionId, Pageable pageable) {
        return executionRecordRepository.findBySessionIdOrderByCreatedAtDesc(sessionId, pageable);
    }

    public List<VariableInfoDto> getSessionVariables(String sessionId) {
        sessionService.validateSession(sessionId);
        SessionStateResponse response = pythonMarimoServiceClient.getSessionState(sessionId);

        return response.getStateMap().entrySet().stream()
                .map(entry -> new VariableInfoDto(
                        entry.getKey(),
                        "unknown", 
                        entry.getValue(),
                        0,      
                        null    
                ))
                .collect(Collectors.toList());
    }

    public void updateWidgetValue(String sessionId, String widgetId, String value) {
        sessionService.validateSession(sessionId);
        
        // Get widget state to determine type for validation
        var widgetState = widgetStateRepository.findBySessionIdAndWidgetId(sessionId, widgetId);
        if (widgetState.isPresent()) {
            String widgetType = widgetState.get().getWidgetType();
            
            // Validate value based on widget type
            validateWidgetValue(widgetType, value, widgetId);
        }
        
        pythonMarimoServiceClient.updateWidgetValue(sessionId, widgetId, value);
    }

    public BatchUpdateResponseDto batchUpdateWidgets(String sessionId, List<BatchUpdateWidgetsRequestDto.WidgetUpdateDto> updates) {
        sessionService.validateSession(sessionId);
        
        BatchUpdateResponseDto response = new BatchUpdateResponseDto();
        response.setSuccess(new ArrayList<>());
        response.setFailed(new ArrayList<>());
        response.setTotal(updates.size());
        
        for (BatchUpdateWidgetsRequestDto.WidgetUpdateDto update : updates) {
            try {
                // Get widget state to determine type for validation
                var widgetState = widgetStateRepository.findBySessionIdAndWidgetId(sessionId, update.getWidgetId());
                if (widgetState.isPresent()) {
                    String widgetType = widgetState.get().getWidgetType();
                    
                    // Validate value based on widget type
                    validateWidgetValue(widgetType, update.getValue(), update.getWidgetId());
                }
                
                pythonMarimoServiceClient.updateWidgetValue(sessionId, update.getWidgetId(), update.getValue());
                response.getSuccess().add(update.getWidgetId());
            } catch (Exception e) {
                BatchUpdateResponseDto.FailedUpdateDto failedUpdate = new BatchUpdateResponseDto.FailedUpdateDto();
                failedUpdate.setWidgetId(update.getWidgetId());
                failedUpdate.setError(e.getMessage());
                response.getFailed().add(failedUpdate);
            }
        }
        
        return response;
    }

    public WidgetAnalyticsDto getWidgetAnalytics(String sessionId) {
        sessionService.validateSession(sessionId);
        
        // For now, return mock data. This could be extended to get real analytics from the Python service
        WidgetAnalyticsDto analytics = new WidgetAnalyticsDto();
        analytics.setTotalWidgets(0);
        analytics.setWidgetTypes(new HashMap<>());
        analytics.setTotalUpdates(0);
        analytics.setPerformanceMetrics(new HashMap<>());
        
        return analytics;
    }

    public WidgetStateDto getWidgetState(String sessionId, String widgetId) {
        sessionService.validateSession(sessionId);
        
        // For now, return mock data. This could be extended to get real state from the Python service
        WidgetStateDto state = new WidgetStateDto();
        state.setWidgetId(widgetId);
        state.setType("unknown");
        state.setValue("");
        state.setProperties(new HashMap<>());
        state.setLoading(false);
        state.setError(null);
        state.setLastUpdated(System.currentTimeMillis());
        state.setUpdateCount(0);
        
        return state;
    }

    public WidgetConstraintsDto getWidgetConstraints(String sessionId, String widgetId) {
        sessionService.validateSession(sessionId);
        
        try {
            // Look up the widget state to get its type
            var widgetState = widgetStateRepository.findBySessionIdAndWidgetId(sessionId, widgetId);
            
            WidgetConstraintsDto constraints = new WidgetConstraintsDto();
            Map<String, Object> constraintsMap = new HashMap<>();
            
            if (widgetState.isPresent()) {
                WidgetState state = widgetState.get();
                String widgetType = state.getWidgetType();
                constraints.setType(widgetType);
                
                // Set constraints based on widget type
                switch (widgetType) {
                    case "slider":
                        constraintsMap.put("min", 0);
                        constraintsMap.put("max", 100);
                        constraintsMap.put("step", 1);
                        break;
                    case "range_slider":
                        constraintsMap.put("min", 0);
                        constraintsMap.put("max", 100);
                        constraintsMap.put("step", 1);
                        constraintsMap.put("minDistance", 0);
                        break;
                    case "number":
                        constraintsMap.put("min", Double.NEGATIVE_INFINITY);
                        constraintsMap.put("max", Double.POSITIVE_INFINITY);
                        constraintsMap.put("step", 0.1);
                        break;
                    case "text":
                        constraintsMap.put("maxLength", 1000);
                        constraintsMap.put("pattern", ".*");
                        break;
                    case "select":
                    case "dropdown":
                        constraintsMap.put("multiple", false);
                        constraintsMap.put("options", new ArrayList<>());
                        break;
                    case "multiselect":
                        constraintsMap.put("multiple", true);
                        constraintsMap.put("options", new ArrayList<>());
                        constraintsMap.put("maxSelections", 10);
                        break;
                    case "checkbox":
                        constraintsMap.put("required", false);
                        break;
                    case "radio":
                        constraintsMap.put("required", true);
                        constraintsMap.put("options", new ArrayList<>());
                        break;
                    case "date":
                        constraintsMap.put("format", "YYYY-MM-DD");
                        break;
                    case "file":
                        constraintsMap.put("maxSize", 10485760); // 10MB
                        constraintsMap.put("acceptedTypes", List.of("*/*"));
                        break;
                    default:
                        constraintsMap.put("type", "unknown");
                }
                
                // Load any stored constraints from the database
                if (state.getConstraints() != null && !state.getConstraints().isEmpty()) {
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> storedConstraints = objectMapper.readValue(state.getConstraints(), Map.class);
                        constraintsMap.putAll(storedConstraints);
                    } catch (JsonProcessingException e) {
                        log.warn("Failed to parse stored constraints for widget: sessionId={}, widgetId={}, error={}", 
                                sessionId, widgetId, e.getMessage());
                    }
                }
            } else {
                constraints.setType("unknown");
                constraintsMap.put("type", "unknown");
            }
            
            constraints.setConstraints(constraintsMap);
            
            log.info("Retrieved widget constraints: sessionId={}, widgetId={}, type={}", 
                    sessionId, widgetId, constraints.getType());
            
            return constraints;
        } catch (Exception e) {
            log.error("Failed to get widget constraints: sessionId={}, widgetId={}, error={}", 
                    sessionId, widgetId, e.getMessage());
            
            // Return fallback constraints
            WidgetConstraintsDto fallback = new WidgetConstraintsDto();
            fallback.setType("unknown");
            fallback.setConstraints(new HashMap<>());
            return fallback;
        }
    }

    public boolean saveWidgetState(String sessionId, String widgetId, Map<String, Object> state) {
        sessionService.validateSession(sessionId);
        
        try {
            // Convert state map to JSON string
            String stateJson = objectMapper.writeValueAsString(state);
            
            // Check if widget state already exists
            var existingState = widgetStateRepository.findBySessionIdAndWidgetId(sessionId, widgetId);
            
            if (existingState.isPresent()) {
                // Update existing widget state
                WidgetState widgetState = existingState.get();
                widgetState.setValue(stateJson);
                widgetState.setVersion(widgetState.getVersion() + 1);
                widgetStateRepository.save(widgetState);
                
                log.info("Updated widget state: sessionId={}, widgetId={}, version={}", 
                        sessionId, widgetId, widgetState.getVersion());
            } else {
                // Create new widget state
                WidgetState widgetState = new WidgetState(sessionId, widgetId, 
                        (String) state.getOrDefault("type", "unknown"), stateJson);
                widgetStateRepository.save(widgetState);
                
                log.info("Created new widget state: sessionId={}, widgetId={}", sessionId, widgetId);
            }
            
            return true;
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize widget state to JSON: sessionId={}, widgetId={}, error={}", 
                    sessionId, widgetId, e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Failed to save widget state: sessionId={}, widgetId={}, error={}", 
                    sessionId, widgetId, e.getMessage());
            return false;
        }
    }

    public Map<String, Object> loadWidgetState(String sessionId, String widgetId) {
        sessionService.validateSession(sessionId);
        
        try {
            var widgetState = widgetStateRepository.findBySessionIdAndWidgetId(sessionId, widgetId);
            
            if (widgetState.isPresent()) {
                WidgetState state = widgetState.get();
                
                // Parse JSON string back to Map
                @SuppressWarnings("unchecked")
                Map<String, Object> stateMap = objectMapper.readValue(state.getValue(), Map.class);
                
                log.info("Loaded widget state: sessionId={}, widgetId={}, version={}", 
                        sessionId, widgetId, state.getVersion());
                
                return stateMap;
            } else {
                log.info("No widget state found: sessionId={}, widgetId={}", sessionId, widgetId);
                return new HashMap<>();
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to parse widget state JSON: sessionId={}, widgetId={}, error={}", 
                    sessionId, widgetId, e.getMessage());
            return new HashMap<>();
        } catch (Exception e) {
            log.error("Failed to load widget state: sessionId={}, widgetId={}, error={}", 
                    sessionId, widgetId, e.getMessage());
            return new HashMap<>();
        }
    }

    public boolean deleteWidgetState(String sessionId, String widgetId) {
        sessionService.validateSession(sessionId);
        
        try {
            var widgetState = widgetStateRepository.findBySessionIdAndWidgetId(sessionId, widgetId);
            
            if (widgetState.isPresent()) {
                WidgetState state = widgetState.get();
                
                // Soft delete by setting active to false
                state.setActive(false);
                widgetStateRepository.save(state);
                
                log.info("Deleted widget state: sessionId={}, widgetId={}", sessionId, widgetId);
                return true;
            } else {
                log.info("Widget state not found for deletion: sessionId={}, widgetId={}", sessionId, widgetId);
                return false;
            }
        } catch (Exception e) {
            log.error("Failed to delete widget state: sessionId={}, widgetId={}, error={}", 
                    sessionId, widgetId, e.getMessage());
            return false;
        }
    }

    public List<WidgetTemplateDto> getWidgetTemplates(String category) {
        // TODO: Implement template retrieval logic
        // For now, return empty list
        log.info("Getting widget templates: category={}", category);
        
        return new ArrayList<>();
    }

    public WidgetTemplateDto saveWidgetTemplate(WidgetTemplateDto template) {
        // TODO: Implement template saving logic
        log.info("Saving widget template: {}", template.getId());
        
        return template;
    }

    public boolean deleteWidgetTemplate(String templateId) {
        // TODO: Implement template deletion logic
        log.info("Deleting widget template: {}", templateId);
        
        return true;
    }

    public List<WidgetVersionDto> getWidgetVersions(String sessionId, String widgetId) {
        sessionService.validateSession(sessionId);
        
        // TODO: Implement version retrieval logic
        log.info("Getting widget versions: sessionId={}, widgetId={}", sessionId, widgetId);
        
        return new ArrayList<>();
    }

    public WidgetVersionDto createWidgetVersion(String sessionId, String widgetId, Map<String, Object> version) {
        sessionService.validateSession(sessionId);
        
        // TODO: Implement version creation logic
        log.info("Creating widget version: sessionId={}, widgetId={}, version={}", sessionId, widgetId, version);
        
        WidgetVersionDto versionDto = new WidgetVersionDto();
        versionDto.setWidgetId(widgetId);
        versionDto.setVersion(1);
        versionDto.setTimestamp(System.currentTimeMillis());
        
        return versionDto;
    }

    public boolean revertWidgetToVersion(String sessionId, String widgetId, int versionId) {
        sessionService.validateSession(sessionId);
        
        // TODO: Implement version reversion logic
        log.info("Reverting widget to version: sessionId={}, widgetId={}, versionId={}", sessionId, widgetId, versionId);
        
        return true;
    }

    public WidgetCollaborationDto getSessionCollaborators(String sessionId) {
        sessionService.validateSession(sessionId);
        
        // TODO: Implement collaboration retrieval logic
        log.info("Getting session collaborators: sessionId={}", sessionId);
        
        WidgetCollaborationDto collaboration = new WidgetCollaborationDto();
        collaboration.setSessionId(sessionId);
        collaboration.setCollaborators(new ArrayList<>());
        collaboration.setWidgetLocks(new HashMap<>());
        collaboration.setRecentActivity(new ArrayList<>());
        
        return collaboration;
    }

    public boolean requestWidgetLock(String sessionId, String widgetId, String userId) {
        sessionService.validateSession(sessionId);
        
        // TODO: Implement widget locking logic
        log.info("Requesting widget lock: sessionId={}, widgetId={}, userId={}", sessionId, widgetId, userId);
        
        return true;
    }

    public boolean releaseWidgetLock(String sessionId, String widgetId, String userId) {
        sessionService.validateSession(sessionId);
        
        // TODO: Implement widget unlocking logic
        log.info("Releasing widget lock: sessionId={}, widgetId={}, userId={}", sessionId, widgetId, userId);
        
        return true;
    }

    /**
     * Validate widget value based on widget type
     */
    private void validateWidgetValue(String widgetType, String value, String widgetId) {
        if (value == null) {
            return; // Allow null values
        }

        try {
            switch (widgetType) {
                case "number":
                    // Validate number values
                    if (value.trim().isEmpty()) {
                        throw new IllegalArgumentException("Number value cannot be empty");
                    }
                    
                    try {
                        Double.parseDouble(value);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Invalid number format: '" + value + "'");
                    }
                    break;
                    
                case "range_slider":
                    // Validate range slider values (should be JSON array)
                    try {
                        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        double[] range = mapper.readValue(value, double[].class);
                        if (range.length != 2) {
                            throw new IllegalArgumentException("Range slider must have exactly 2 values");
                        }
                        if (range[0] > range[1]) {
                            throw new IllegalArgumentException("Range slider min value cannot be greater than max value");
                        }
                    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                        throw new IllegalArgumentException("Invalid range slider format: must be JSON array of 2 numbers");
                    }
                    break;
                    
                case "checkbox":
                    // Validate boolean values
                    if (!value.equals("true") && !value.equals("false")) {
                        throw new IllegalArgumentException("Checkbox value must be 'true' or 'false'");
                    }
                    break;
                    
                case "multiselect":
                    // Validate multiselect values (should be JSON array)
                    try {
                        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        String[] values = mapper.readValue(value, String[].class);
                        // Additional validation could be added here to check against allowed options
                    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                        throw new IllegalArgumentException("Invalid multiselect format: must be JSON array of strings");
                    }
                    break;
                    
                // For other widget types (text, dropdown, radio, select, etc.), allow any string value
                default:
                    // No specific validation needed
                    break;
            }
            
            log.debug("Widget value validation passed: widgetId={}, type={}, value={}", widgetId, widgetType, value);
            
        } catch (Exception e) {
            log.error("Widget value validation failed: widgetId={}, type={}, value={}, error={}", 
                    widgetId, widgetType, value, e.getMessage());
            throw new IllegalArgumentException("Invalid value for " + widgetType + " widget: " + e.getMessage());
        }
    }

    private CellOutputDto toDto(CellOutput proto) {
        return new CellOutputDto(
                null,
                proto.getType().name(),
                proto.getContent(),
                proto.getData().toByteArray(),
                proto.getMimeType(),
                proto.getMetadataMap(),
                null,
                proto.getDataType().name()
        );
    }
} 