package olsh.backend.api_gateway.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LabResponse {
    private Long id;
    private String title;
    private String shortDesc;
    private String createdAt;
    private Long views;
    private Long submissions;
    private Long authorId;
    private String authorName;
    private String authorSurname;
}
