package olsh.backend.marimomanagerservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthValidationResponse {
    private boolean valid;
    private UserInfo userInfo;
    private Long expirationTime;
    private String errorMessage;
} 