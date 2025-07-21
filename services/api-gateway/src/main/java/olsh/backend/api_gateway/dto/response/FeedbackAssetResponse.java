package olsh.backend.api_gateway.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response containing feedback attachment metadata")
public class FeedbackAssetResponse {

    @Schema(description = "Unique identifier of the feedback this asset belongs to")
    @JsonProperty("feedback_id")
    private String feedbackId;

    @Schema(description = "Original name of the uploaded file")
    @JsonProperty("filename")
    private String filename;

    @Schema(description = "MIME type of the file (e.g., 'application/pdf', 'image/png')")
    @JsonProperty("content_type")
    private String contentType;

    @Schema(description = "Size of the file in bytes")
    @JsonProperty("total_size")
    private Long totalSize;
} 