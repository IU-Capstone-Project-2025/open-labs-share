package olsh.backend.api_gateway.dto.response;

import lombok.Builder;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Builder
@Schema(description = "Response object containing asset details")
public class SubmissionAssetResponse {

    @Schema(description = "Unique identifier of the asset", example = "1")
    private Long assetId;

    @Schema(description = "ID of the submission this asset belongs to", example = "1")
    private Long submissionId;

    @Schema(description = "Original filename of the asset", example = "solution.java")
    private String filename;

    @Schema(description = "Size of the file in bytes", example = "2048")
    private Long totalSize;

    @Schema(description = "Upload date in ISO 8601 format", example = "2024-03-15T14:30:00Z")
    private String uploadDate;
}

