package olsh.backend.marimomanagerservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserInfo {
    private Long id;
    private String username;
    private String firstName;
    private String lastName;
    private String role;
    private String email;
    private Integer labs_solved;
    private Integer labs_reviewed;
    private Integer balance;
} 