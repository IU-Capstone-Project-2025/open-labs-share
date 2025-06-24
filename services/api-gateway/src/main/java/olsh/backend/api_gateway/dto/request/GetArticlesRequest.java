package olsh.backend.api_gateway.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetArticlesRequest {

    @Min(value = 1, message = "Page must be greater than 0")
    private Integer page = 1;

    @Min(value = 1, message = "Limit must be greater than 0")
    @Max(value = 100, message = "Limit must not exceed 100")
    private Integer limit = 20;
}

