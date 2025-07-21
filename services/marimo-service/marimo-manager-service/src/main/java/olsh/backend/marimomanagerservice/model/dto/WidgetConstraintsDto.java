package olsh.backend.marimomanagerservice.model.dto;

import lombok.Data;
import java.util.Map;

@Data
public class WidgetConstraintsDto {
    private String type;
    private Map<String, Object> constraints;
}
