package olsh.backend.marimomanagerservice.model.dto;

import lombok.Data;
import java.util.List;

@Data
public class BatchUpdateWidgetsRequestDto {
    private List<WidgetUpdateDto> updates;
    
    @Data
    public static class WidgetUpdateDto {
        private String widgetId;
        private String value;
        private String priority; // "high", "normal", "low"
    }
}
