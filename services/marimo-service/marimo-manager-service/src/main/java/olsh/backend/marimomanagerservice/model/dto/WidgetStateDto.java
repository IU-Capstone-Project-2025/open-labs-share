package olsh.backend.marimomanagerservice.model.dto;

import lombok.Data;
import java.util.Map;

@Data
public class WidgetStateDto {
    private String widgetId;
    private String type;
    private String value;
    private Map<String, Object> properties;
    private boolean isLoading;
    private String error;
    private long lastUpdated;
    private int updateCount;
}
