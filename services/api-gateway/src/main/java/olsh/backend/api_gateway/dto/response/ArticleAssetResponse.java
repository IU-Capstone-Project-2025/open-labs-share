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
@Schema(description = "Response object containing article asset details")
public class ArticleAssetResponse {

    @Schema(description = "Unique identifier of the asset")
    private Long assetId;

    @Schema(description = "Identifier of the related article")
    private Long articleId;

    @Schema(description = "Name of the file")
    private String filename;

    @Schema(description = "Size of the file in bytes")
    private Long filesize;

    @Schema(description = "Date and time when the asset was uploaded")
    private String uploadDate;
}
