package olsh.backend.marimomanagerservice.service;

import lombok.RequiredArgsConstructor;
import olsh.backend.marimomanagerservice.grpc.client.PythonMarimoServiceClient;
import olsh.backend.grpc.marimo.*;
import olsh.backend.marimomanagerservice.repository.ExecutionRecordRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import olsh.backend.marimomanagerservice.entity.ExecutionRecord;
import olsh.backend.marimomanagerservice.model.dto.CellOutputDto;
import olsh.backend.marimomanagerservice.model.dto.ExecuteCellResponseDto;
import olsh.backend.marimomanagerservice.model.dto.VariableInfoDto;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExecutionService {

    private final PythonMarimoServiceClient pythonMarimoServiceClient;
    private final SessionService sessionService;
    private final ExecutionRecordRepository executionRecordRepository;

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