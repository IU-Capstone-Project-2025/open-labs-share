package olsh.backend.marimomanagerservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComponentDto {
    private String id;
    private String name;
    private String contentType;
    private String contentId;
    private String ownerId;
    private String notebookPath;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
} 