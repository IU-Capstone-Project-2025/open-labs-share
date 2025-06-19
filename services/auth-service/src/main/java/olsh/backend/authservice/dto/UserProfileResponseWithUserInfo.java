package olsh.backend.authservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User profile information with reused UserInfo")
public class UserProfileResponseWithUserInfo {
    @Schema(description = "Basic user information")
    private UserInfo userInfo;
    
    @Schema(description = "Email address", example = "johndoe@mail.com")
    private String email;
    
    @Schema(description = "Account creation timestamp", example = "2023-01-15T10:30:00")
    private String createdAt;

    @Schema(description = "Last login timestamp", example = "2023-01-20T14:45:00")
    private String lastLoginAt;

    @Schema(description = "Account status", example = "ACTIVE")
    private String status;
}
