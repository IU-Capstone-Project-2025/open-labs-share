package olsh.backend.api_gateway.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import olsh.backend.api_gateway.dto.response.UserResponse;
import olsh.backend.api_gateway.grpc.model.AuthValidationResponse;
import olsh.backend.api_gateway.service.AuthService;
import olsh.backend.api_gateway.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@CrossOrigin(origins = "*", maxAge = 3600)
public class UserController {

    private final AuthService authService;
    private final UserService userService;

    @Autowired
    public UserController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @GetMapping("/{user_id}")
    public ResponseEntity<UserResponse> getUser(
            @PathVariable("user_id") Long userId,
            Authentication authentication) {

        log.debug("Received request to get user with ID: {}", userId);

        // Get user data via user service
        UserResponse userResponse = userService.getUserById(userId);

        log.debug("Successfully retrieved user data for userId: {}", userId);
        return ResponseEntity.ok(userResponse);
    }

}