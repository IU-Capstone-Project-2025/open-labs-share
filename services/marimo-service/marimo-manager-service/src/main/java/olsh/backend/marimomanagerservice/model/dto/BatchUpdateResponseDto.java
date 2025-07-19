package olsh.backend.marimomanagerservice.model.dto;

import lombok.Data;
import java.util.List;

@Data
public class BatchUpdateResponseDto {
    private List<String> success;
    private List<FailedUpdateDto> failed;
    private int total;
    
    @Data
    public static class FailedUpdateDto {
        private String widgetId;
        private String error;
    }
}
