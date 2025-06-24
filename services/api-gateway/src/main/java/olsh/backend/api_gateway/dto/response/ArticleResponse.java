package olsh.backend.api_gateway.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArticleResponse {
    private Long id;
    private String title;
    private String shortDesc;
    private String createdAt;
    private Long views;
    private Long authorId;
    private String authorName;
    private String authorSurname;
}

