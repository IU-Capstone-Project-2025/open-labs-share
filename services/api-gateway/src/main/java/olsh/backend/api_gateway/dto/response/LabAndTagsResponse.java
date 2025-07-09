package olsh.backend.api_gateway.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@Schema(description = "Response object containing laboratory work details")
public class LabAndTagsResponse {
    @Schema(description = "Unique identifier of the lab", example = "1")
    private Long id;

    @Schema(description = "Title of the lab", example = "Introduction to Data Structures")
    private String title;

    @Schema(description = "Short description of the lab", example = "Learn about basic data structures and their implementations")
    private String shortDesc;

    @Schema(description = "Creation date and time in ISO 8601 format", example = "2024-03-15T14:30:00Z")
    private String createdAt;

    @Schema(description = "Number of times the lab has been viewed", example = "42")
    private Long views;

    @Schema(description = "Number of submissions for this lab", example = "15")
    private Long submissions;

    @Schema(description = "Unique identifier of the lab's author", example = "123")
    private Long authorId;

    @Schema(description = "First name of the lab's author", example = "John")
    private String authorName;

    @Schema(description = "Last name of the lab's author", example = "Doe")
    private String authorSurname;

    @Schema(description = "List of assets associated with this lab", example = "See AssetResponse")
    private List<AssetResponse> assets;

    @Schema(description = "List of article IDs associated with this lab", example = "[1, 2, 3]")
    private List<Long> articles;

    @Schema(description = "List of tags associated with this lab", example = "See TagResponse")
    private List<TagResponse> tags;
}
