package olsh.backend.api_gateway.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Response object containing statistics about the system")
public class StatisticsResponse {

    @Schema(description = "Number of users", example = "1000")
    private Integer users;

    @Schema(description = "Number of labs", example = "50")
    private Integer labs;

    @Schema(description = "Number of articles", example = "200")
    private Integer articles;

    @Schema(description = "Number of submissions", example = "5000")
    private Integer submissions;
}
