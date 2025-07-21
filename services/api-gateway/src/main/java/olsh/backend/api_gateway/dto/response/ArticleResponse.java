package olsh.backend.api_gateway.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Response object containing article details")
public class ArticleResponse {
    @Schema(description = "Unique identifier of the article", example = "1")
    private Long id;

    @Schema(description = "Title of the article", example = "Introduction to Machine Learning")
    private String title;

    @Schema(description = "Short description of the article", example = "A comprehensive guide to machine learning basics")
    private String shortDesc;

    @Schema(description = "Creation date and time in ISO 8601 format", example = "2024-03-15T14:30:00Z")
    private String createdAt;

    @Schema(description = "Number of times the article has been viewed", example = "42")
    private Long views;

    @Schema(description = "Unique identifier of the article's author", example = "123")
    private Long authorId;

    @Schema(description = "First name of the article's author", example = "John")
    private String authorName;

    @Schema(description = "Last name of the article's author", example = "Doe")
    private String authorSurname;

    @Schema(description = "Asset details associated with the article")
    private ArticleAssetResponse asset;
}

