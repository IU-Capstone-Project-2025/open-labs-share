package olsh.backend.api_gateway.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Data
@Schema(description = "Request object for creating a new laboratory work")
public class LabCreateRequest {
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

    @Schema(description = "List of article IDs to associate with this lab", example = "[1,2,4]", required = false)
    private String articles;

    @Schema(description = "Comma-separated list of tag IDs", example = "[5,6,7]", required = false)
    private String tags;

    // Add a helper method to convert to Long[]
    private List<Long> getStrAsArray(String str) {
        if (str == null || str.trim().isEmpty()) {
            return Collections.emptyList();
        }
        // Handle both "[6,5,4]" and "6,5,4" formats
        String cleanTags = str.replaceAll("[\\[\\]]", "").trim();
        if (cleanTags.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(cleanTags.split(","))
                .map(String::trim)
                .map(Long::parseLong).toList();
    }

    public List<Long> getArticlesAsArray() {
        return getStrAsArray(this.articles);
    }

    public List<Long> getTagsAsArray() {
        return getStrAsArray(this.tags);
    }
}

