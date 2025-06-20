package olsh.backend.api_gateway.grpc.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserData {
    private Long id;
    private String username;
    private String name;
    private String surname;
    private String email;
    private boolean found;
}
