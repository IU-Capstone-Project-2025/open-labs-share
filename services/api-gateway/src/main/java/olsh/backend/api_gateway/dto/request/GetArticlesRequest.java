package olsh.backend.api_gateway.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request object for retrieving a paginated list of articles")
public class GetArticlesRequest {

    @Schema(description = "Page number (1-based)", example = "1", defaultValue = "1", minimum = "1")
    @Min(value = 1, message = "Page must be greater than 0")
    private Integer page = 1;

    @Schema(description = "Number of items per page", example = "20", defaultValue = "20", minimum = "1", maximum = "100")
    @Min(value = 1, message = "Limit must be greater than 0")
    @Max(value = 100, message = "Limit must not exceed 100")
    private Integer limit = 20;
}

