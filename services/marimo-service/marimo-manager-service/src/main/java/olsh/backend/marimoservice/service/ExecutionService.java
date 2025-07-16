package olsh.backend.marimoservice.service;

import lombok.RequiredArgsConstructor;
import olsh.backend.marimoservice.grpc.client.PythonMarimoServiceClient;
import olsh.backend.marimoservice.grpc.proto.PythonMarimoServiceProto;
import olsh.backend.marimoservice.repository.ExecutionRecordRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import olsh.backend.marimoservice.entity.ExecutionRecord;
import olsh.backend.marimoservice.model.dto.CellOutputDto;
import olsh.backend.marimoservice.model.dto.ExecuteCellResponseDto;
import olsh.backend.marimoservice.model.dto.VariableInfoDto;

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
        PythonMarimoServiceProto.ExecuteResponse pythonResponse = pythonMarimoServiceClient.executeCell(sessionId, cellId, code);
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
        PythonMarimoServiceProto.SessionStateResponse response = pythonMarimoServiceClient.getSessionState(sessionId);

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

    private CellOutputDto toDto(PythonMarimoServiceProto.CellOutput proto) {
        return new CellOutputDto(
                null,
                proto.getType().name(),
                proto.getContent(),
                proto.getData().toByteArray(),
                proto.getMimeType(),
                proto.getMetadataMap(),
                null
        );
    }
} 