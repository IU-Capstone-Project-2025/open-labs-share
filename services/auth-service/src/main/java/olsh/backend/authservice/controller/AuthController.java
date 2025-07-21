package olsh.backend.authservice.controller;

import java.security.Principal;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import olsh.backend.authservice.dto.AuthenticationResponse;

import olsh.backend.authservice.dto.PasswordResetConfirmRequest;
import olsh.backend.authservice.dto.PasswordResetRequest;
import olsh.backend.authservice.dto.RefreshTokenRequest;
import olsh.backend.authservice.dto.SignInRequest;
import olsh.backend.authservice.dto.SignUpRequest;
import olsh.backend.authservice.dto.UpdateProfileRequest;
import olsh.backend.authservice.dto.UpdateProfileResponse;
import olsh.backend.authservice.dto.UserProfileResponseWithUserInfo;
import olsh.backend.authservice.service.AuthenticationService;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = {"http://localhost:5173", "https://open-labs-share.online"}, allowCredentials = "true", maxAge = 3600)
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication and authorization endpoints")
public class AuthController {
    private final AuthenticationService authenticationService;

    @Operation(
        summary = "Register new user",
        description = "Creates a new user account and returns JWT tokens"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "User registered successfully",
            content = @Content(schema = @Schema(implementation = AuthenticationResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "409", description = "User already exists")
    })
    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(
        @RequestBody @Valid SignUpRequest request) {
        log.info("User registration attempt for username: {}", request.getUsername());
        AuthenticationResponse response = authenticationService.signUp(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
        summary = "Authenticate user",
        description = "Authenticates user credentials and returns JWT tokens"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Authentication successful",
            content = @Content(schema = @Schema(implementation = AuthenticationResponse.class))),
        @ApiResponse(responseCode = "401", description = "Invalid credentials"),
        @ApiResponse(responseCode = "423", description = "Account locked")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(
        @RequestBody @Valid SignInRequest request,
        HttpServletRequest httpRequest) {
        log.info("Login attempt for username/email: {}", request.getUsernameOrEmail());
        AuthenticationResponse response = authenticationService.signIn(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Refresh access token",
        description = "Generates new access token using refresh token"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token refreshed successfully",
            content = @Content(schema = @Schema(implementation = AuthenticationResponse.class))),
        @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    })
    @PostMapping("/refresh")
    public ResponseEntity<AuthenticationResponse> refreshToken(
        @RequestBody @Valid RefreshTokenRequest request) {
        log.debug("Token refresh attempt");
        AuthenticationResponse response = authenticationService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Logout user",
        description = "Invalidates the current JWT token and logs out the user",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Logout successful",
            content = @Content(schema = @Schema(implementation = olsh.backend.authservice.dto.ApiResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<olsh.backend.authservice.dto.ApiResponse> logout(
        HttpServletRequest request, Principal principal) {
        log.info("Logout request for user: {}", principal.getName());
        String token = extractTokenFromRequest(request);
        authenticationService.logout(token);
        return ResponseEntity.ok(
            olsh.backend.authservice.dto.ApiResponse.builder()
                .success(true)
                .message("Successfully logged out")
                .build()
        );
    }

    @Operation(
        summary = "Request password reset ⚠️ NOT YET IMPLEMENTED",
        description = "Sends password reset instructions to user's email (functionality not yet implemented)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Password reset email sent",
            content = @Content(schema = @Schema(implementation = olsh.backend.authservice.dto.ApiResponse.class))),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PostMapping("/password-reset")
    public ResponseEntity<olsh.backend.authservice.dto.ApiResponse> requestPasswordReset(
        @RequestBody @Valid PasswordResetRequest request) {
        log.info("Password reset requested for email: {}", request.getEmail());
        authenticationService.requestPasswordReset(request);
        return ResponseEntity.ok(
            olsh.backend.authservice.dto.ApiResponse.builder()
                .success(true)
                .message("Password reset instructions sent to your email")
                .build()
        );
    }

    @Operation(
        summary = "Reset password ⚠️ NOT YET IMPLEMENTED",
        description = "Resets user password using reset token (functionality not yet implemented)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Password reset successful",
            content = @Content(schema = @Schema(implementation = olsh.backend.authservice.dto.ApiResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid or expired reset token")
    })
    @PostMapping("/password-reset/confirm")
    public ResponseEntity<olsh.backend.authservice.dto.ApiResponse> confirmPasswordReset(
        @RequestBody @Valid PasswordResetConfirmRequest request) {
        log.info("Password reset confirmation attempt");
        authenticationService.confirmPasswordReset(request);
        return ResponseEntity.ok(
            olsh.backend.authservice.dto.ApiResponse.builder()
                .success(true)
                .message("Password reset successfully")
                .build()
        );
    }

    @Operation(
        summary = "Get user profile",
        description = "Retrieves authenticated user's profile information",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Profile retrieved successfully",
            content = @Content(schema = @Schema(implementation = UserProfileResponseWithUserInfo.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileResponseWithUserInfo> getProfile(Principal principal) {
        log.debug("Profile request for user: {}", principal.getName());
        UserProfileResponseWithUserInfo profile =
            authenticationService.getUserProfileWithUserInfo(principal.getName());
        return ResponseEntity.ok(profile);
    }

    @Operation(
        summary = "Update user profile",
        description = "Updates authenticated user's profile information. Returns new JWT tokens if username was changed, same tokens otherwise.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Profile updated successfully",
            content = @Content(schema = @Schema(implementation = UpdateProfileResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data or validation error"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "409", description = "Username or email already exists")
    })
    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UpdateProfileResponse> updateProfile(
        @RequestBody @Valid UpdateProfileRequest request,
        Principal principal,
        HttpServletRequest httpRequest) {
        log.info("Profile update request for user: {}", principal.getName());
        
        String currentToken = extractTokenFromRequest(httpRequest);
        UpdateProfileResponse response = authenticationService.updateProfile(request, principal.getName(), currentToken);
        
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Verify email address ⚠️ NOT YET IMPLEMENTED",
        description = "Verifies user's email address using verification token (functionality not yet implemented)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Email verified successfully",
            content = @Content(schema = @Schema(implementation = olsh.backend.authservice.dto.ApiResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid or expired verification token")
    })
    @GetMapping("/verify-email/{token}")
    public ResponseEntity<olsh.backend.authservice.dto.ApiResponse> verifyEmail(
        @PathVariable @Parameter(description = "Email verification token") String token) {
        log.info("Email verification attempt with token: {}", token);
        authenticationService.verifyEmail(token);
        return ResponseEntity.ok(
            olsh.backend.authservice.dto.ApiResponse.builder()
                .success(true)
                .message("Email verified successfully")
                .build()
        );
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
