package olsh.backend.api_gateway.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request object for creating a new article")
public class CreateArticleRequest {

    @Schema(description = "Title of the article", example = "Introduction to Machine Learning", required = true)
    @NotBlank(message = "Title is required")
    @Size(min = 10, max = 255, message = "Title must be 10 to 255 characters")
    private String title;

    @Schema(description = "Short description of the article", example = "A comprehensive guide to machine learning basics", required = true)
    @NotBlank(message = "Short description is required")
    @Size(min = 20, max = 1000, message = "Short description must be 20 to 1000 characters")
    private String short_desc;

    @Schema(description = "PDF file containing the article content", required = true)
    @NotNull(message = "PDF file is required")
    private MultipartFile pdf_file;
}

