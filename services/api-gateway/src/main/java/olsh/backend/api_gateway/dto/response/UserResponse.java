package olsh.backend.api_gateway.dto.response;


public record UserResponse(
        Long id,
        String username,
        String name,
        String surname,
        String email
) {
}
