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

    @Schema(description = "Account status", example = "ACTIVE")
    private String status;
}
