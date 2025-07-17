package olsh.backend.api_gateway.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import olsh.backend.api_gateway.annotation.RequireAuth;
import olsh.backend.api_gateway.dto.response.UserResponse;
import olsh.backend.api_gateway.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@CrossOrigin(origins = {"http://localhost:5173", "https://open-labs-share.online"}, allowCredentials = "true", maxAge = 3600)
@Tag(name = "User Management", description = "Endpoints for managing user information")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(
        summary = "Get user by ID",
        description = "Retrieves detailed information about a user based on their unique identifier. Requires authentication."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "User found and returned successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class))
        ),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @RequireAuth
    @GetMapping("/{user_id}")
    public ResponseEntity<UserResponse> getUser(
            @Parameter(description = "ID of the user to retrieve", required = true)
            @PathVariable("user_id") Long userId,
            HttpServletRequest request) {

        log.debug("Received request to get user with ID: {}", userId);

        // Get user data via user service
        UserResponse userResponse = userService.getUserById(userId);

        log.debug("Successfully retrieved user data for userId: {}", userId);
        return ResponseEntity.ok(userResponse);
    }

}