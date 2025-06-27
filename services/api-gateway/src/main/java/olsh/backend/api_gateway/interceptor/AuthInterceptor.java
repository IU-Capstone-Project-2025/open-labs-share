package olsh.backend.api_gateway.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import olsh.backend.api_gateway.annotation.RequireAuth;
import olsh.backend.api_gateway.grpc.model.AuthValidationResponse;
import olsh.backend.api_gateway.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final AuthService authService;
    @Getter
    private final String ATTRIBUTE_USER = "authenticatedUser";
    @Getter
    private final String ATTRIBUTE_RESPONSE = "authResponse";

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        // Only check methods with @RequireAuth annotation
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true; // Not a controller method, continue
        }

        RequireAuth requireAuth = handlerMethod.getMethodAnnotation(RequireAuth.class);

        // If no @RequireAuth annotation, skip authentication
        if (requireAuth == null) {
            log.debug("No @RequireAuth annotation found for {}, skipping authentication",
                    request.getRequestURI());
            return true;
        }

        log.debug("@RequireAuth annotation found for {}, validating token",
                request.getRequestURI());

        // Check for Bearer token
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("No Bearer token found in request to {}", request.getRequestURI());
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json");
            response.getWriter().write(String.format(
                    "{\"error\":\"Unauthorized\",\"message\":\"%s\"}", requireAuth.message()));
            return false;
        }

        try {
            String token = authHeader.substring(7);
            AuthValidationResponse authResponse = authService.validateToken(token);

            if (!authResponse.isValid()) {
                log.warn("Invalid token for request to {}", request.getRequestURI());
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setContentType("application/json");
                String errorMsg = authResponse.getErrorMessage() != null ?
                        authResponse.getErrorMessage() : requireAuth.message();
                response.getWriter().write(String.format(
                        "{\"error\":\"Unauthorized\",\"message\":\"%s\"}", errorMsg));
                return false;
            }

            // Check roles if specified in annotation
            if (requireAuth.roles().length > 0) {
                String userRole = authResponse.getUserInfo().getRole();
                boolean hasRequiredRole = Arrays.asList(requireAuth.roles()).contains(userRole);

                if (!hasRequiredRole) {
                    log.warn("User {} does not have required role. Has: {}, Required: {}",
                            authResponse.getUserInfo().getUsername(), userRole,
                            Arrays.toString(requireAuth.roles()));
                    response.setStatus(HttpStatus.FORBIDDEN.value());
                    response.setContentType("application/json");
                    response.getWriter().write(
                            "{\"error\":\"Forbidden\",\"message\":\"Insufficient permissions\"}");
                    return false;
                }
            }

            // Store user info for controller access
            // Use request attributes for this
            request.setAttribute(ATTRIBUTE_USER, authResponse.getUserInfo());
            request.setAttribute(ATTRIBUTE_RESPONSE, authResponse);

            log.debug("Authentication successful for user: {} (Role: {})",
                    authResponse.getUserInfo().getUsername(), authResponse.getUserInfo().getRole());
            return true;

        } catch (Exception e) {
            log.error("Authentication failed for request to {}", request.getRequestURI(), e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setContentType("application/json");
            response.getWriter().write(
                    String.format(
                            "{\"error\":\"Authentication Error\",\"message\":\"%s\"}",
                            e.getMessage())
            );
            return false;
        }
    }
}
