package olsh.backend.api_gateway.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Request object for creating a new laboratory work")
public class CreateLabRequest {
    @Schema(description = "Title of the laboratory work", example = "Introduction to Data Structures", required = true)
    @NotBlank(message = "Title is required")
    @Size(min = 10, max = 255, message = "Title must be 10 to 255 characters")
    private String title;

    @Schema(description = "Short description of the laboratory work", example = "Learn about basic data structures and their implementations", required = true)
    @NotBlank(message = "Short description is required")
    @Size(min = 20, max = 1000, message = "Short description must be 20 to 1000 symbols")
    private String short_desc;

    @Schema(description = "Markdown file containing the lab instructions", required = true)
    @NotNull(message = "Markdown file is required")
    private MultipartFile md_file;

    @Schema(description = "Optional supporting files (images in jpeg, png, etc.)", required = false)
    private MultipartFile[] assets;
}

