package olsh.backend.api_gateway.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
@Schema(description = "Request to get tags by a list of IDs")
public class TagsGetByIdsRequest {
    @NotEmpty
    @Schema(description = "List of tag IDs", required = true)
    private List<Integer> ids;
} 