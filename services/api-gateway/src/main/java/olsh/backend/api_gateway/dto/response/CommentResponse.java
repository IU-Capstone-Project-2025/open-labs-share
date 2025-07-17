package olsh.backend.api_gateway.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response object containing details of a single comment")
public class CommentResponse {

    @Schema(description = "Comment ID in UUID format", example = "c1b2c3d4-e5f6-7890-1234-567890abcdef")
    private String id;

    @Schema(description = "Content (lab/article) ID the comment belongs to", example = "123")
    private Long contentId;

    @Schema(description = "User ID who created the comment", example = "456")
    private Long userId;

    @Schema(description = "User's first name", example = "John")
    private String firstName;

    @Schema(description = "User's last name", example = "Doe")
    private String lastName;

    @Schema(description = "ID of the parent comment, if this is a reply", example = "a0b1c2d3-e4f5-6789-0123-456789abcdef")
    private String parentId;

    @Schema(description = "The content of the comment", example = "This is a very insightful lab!")
    private String content;

    @Schema(description = "Creation date and time in ISO 8601 format", example = "2024-01-01T12:00:00Z")
    private String createdAt;

    @Schema(description = "Last update date and time in ISO 8601 format", example = "2024-01-01T12:30:00Z")
    private String updatedAt;
} 