package olsh.backend.marimomanagerservice.model.dto;

import lombok.Data;
import java.util.Map;

@Data
public class WidgetAnalyticsDto {
    private int totalWidgets;
    private Map<String, Integer> widgetTypes;
    private int totalUpdates;
    private Map<String, Object> performanceMetrics;
}
