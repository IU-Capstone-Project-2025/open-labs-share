package olsh.backend.marimomanagerservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteCellResponseDto {
    private String executionId;
    private List<CellOutputDto> outputs;
    private Map<String, String> cellState;
    private long executionTimeMs;
} 