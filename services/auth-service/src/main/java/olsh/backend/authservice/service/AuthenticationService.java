package olsh.backend.authservice.service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import olsh.backend.authservice.dto.AuthenticationResponse;
import olsh.backend.authservice.dto.ChangePasswordRequest;
import olsh.backend.authservice.dto.PasswordResetConfirmRequest;
import olsh.backend.authservice.dto.PasswordResetRequest;
import olsh.backend.authservice.dto.RefreshTokenRequest;
import olsh.backend.authservice.dto.SignInRequest;
import olsh.backend.authservice.dto.SignUpRequest;
import olsh.backend.authservice.dto.TokenValidationResponse;
import olsh.backend.authservice.dto.UserProfileResponse;
import olsh.backend.authservice.dto.ValidateTokenRequest;
import olsh.backend.authservice.entity.Role;
import olsh.backend.authservice.entity.User;
import olsh.backend.authservice.exception.AuthenticationException;
import olsh.backend.authservice.exception.ValidationException;
import olsh.backend.authservice.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthenticationService {
    private final UserService userService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;

    public AuthenticationResponse signUp(SignUpRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new ValidationException("Username already exists");
        }
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ValidationException("Email already exists");
        }

        User user = User.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .role(Role.ROLE_USER)
            .build();

        User savedUser = userService.create(user);
        log.info("User {} created successfully", savedUser.getUsername());

        String accessToken = jwtService.generateToken(savedUser);
        String refreshToken = jwtService.generateRefreshToken(savedUser);

        return buildAuthenticationResponse(savedUser, accessToken, refreshToken);
    }

    public AuthenticationResponse signIn(SignInRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
            .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));

        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                request.getUsername(),
                request.getPassword()
            ));
        } catch (BadCredentialsException e) {
            log.warn("Failed login attempt for user: {}", request.getUsername());
            throw new AuthenticationException("Invalid credentials");
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        log.info("User {} logged in successfully", user.getUsername());
        return buildAuthenticationResponse(user, accessToken, refreshToken);
    }

    public AuthenticationResponse refreshToken(RefreshTokenRequest request) {
        try {
            String username = jwtService.extractUsername(request.getRefreshToken());
            UserDetails userDetails = userService.userDetailsService().loadUserByUsername(username);

            if (jwtService.isTokenValidAndNotBlacklisted(request.getRefreshToken(), userDetails)) {
                User user = (User) userDetails;
                String newAccessToken = jwtService.generateToken(userDetails);
                String newRefreshToken = jwtService.generateRefreshToken(userDetails);

                return buildAuthenticationResponse(user, newAccessToken, newRefreshToken);
            } else {
                throw new AuthenticationException("Invalid refresh token");
            }
        } catch (Exception e) {
            log.error("Error refreshing token: {}", e.getMessage());
            throw new AuthenticationException("Invalid refresh token");
        }
    }

    public TokenValidationResponse validateToken(ValidateTokenRequest request) {
        try {
            String username = jwtService.extractUsername(request.getToken());
            UserDetails userDetails = userService.userDetailsService().loadUserByUsername(username);

            if (jwtService.isTokenBlacklisted(request.getToken())) {
                return TokenValidationResponse.builder()
                    .valid(false)
                    .errorMessage("Token has been invalidated (user logged out)")
                    .build();
            }

            if (jwtService.isTokenValidAndNotBlacklisted(request.getToken(), userDetails)) {
                User user = (User) userDetails;
                Date expiration = jwtService.extractExpiration(request.getToken());

                return TokenValidationResponse.builder()
                    .valid(true)
                    .userId(user.getId())
                    .username(user.getUsername())
                    .role(user.getRole().name())
                    .expirationTime(expiration.getTime())
                    .build();
            } else {
                return TokenValidationResponse.builder()
                    .valid(false)
                    .errorMessage("Token is invalid or expired")
                    .build();
            }
        } catch (Exception e) {
            log.debug("Token validation failed: {}", e.getMessage());
            return TokenValidationResponse.builder()
                .valid(false)
                .errorMessage("Token validation failed: " + e.getMessage())
                .build();
        }
    }

    public void logout(String token) {
        try {
            String username = jwtService.extractUsername(token);
            log.info("User {} logged out", username);
            jwtService.blacklistToken(token);
        } catch (Exception e) {
            log.warn("Error during logout: {}", e.getMessage());
        }
    }

    public void requestPasswordReset(PasswordResetRequest request) {
        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            log.info("Password reset requested for user: {}", user.getUsername());
            // TODO: Implement in future.
        } else {
            log.warn("Password reset requested for non-existent email: {}", request.getEmail());
        }
        log.warn("Password reset functionality not yet implemented!");
    }

    public void confirmPasswordReset(PasswordResetConfirmRequest request) {
        log.info("Password reset confirmed with token: {}", request.getToken());
        // TODO: Implement in future.
        log.warn("Password reset functionality not yet implemented!");
    }

    public void changePassword(ChangePasswordRequest request, String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new ValidationException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("Password changed successfully for user: {}", username);
    }

    public UserProfileResponse getUserProfile(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return UserProfileResponse.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .role(user.getRole().name())
            .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null)
            .lastLoginAt(user.getLastLoginAt() != null ? user.getLastLoginAt().toString() : null)
            .status("ACTIVE")
            .build();
    }

    public void verifyEmail(String token) {
        log.info("Email verification attempted with token: {}", token);
        // TODO: Implement email verification logic.
        log.warn("Email verification functionality not yet implemented");
    }
    
    private AuthenticationResponse buildAuthenticationResponse(User user,
                                                               String accessToken,
                                                               String refreshToken) {
        return AuthenticationResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresAt(LocalDateTime.now().plusHours(24)) // 24-hour expiration
            .userId(user.getId())
            .username(user.getUsername())
            .role(user.getRole().name())
            .build();
    }
}
