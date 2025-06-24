package olsh.backend.api_gateway.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class CreateLabRequest {
    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Short description is required")
    private String short_desc;

    @NotNull(message = "Markdown file is required")
    private MultipartFile md_file;

    private MultipartFile[] assets;
}

