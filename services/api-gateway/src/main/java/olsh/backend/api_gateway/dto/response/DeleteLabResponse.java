package olsh.backend.api_gateway.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DeleteLabResponse {
    private String message;
}

