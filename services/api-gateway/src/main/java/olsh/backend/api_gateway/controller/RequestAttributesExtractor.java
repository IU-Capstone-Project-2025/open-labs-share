package olsh.backend.api_gateway.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import olsh.backend.api_gateway.grpc.model.UserInfo;
import olsh.backend.api_gateway.interceptor.AuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RequestAttributesExtractor {

    private final AuthInterceptor interceptor;

    @Autowired
    public RequestAttributesExtractor(AuthInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    public Long extractUserIdFromRequest(HttpServletRequest request) {
        try {
            // Get the authenticated user info stored by AuthInterceptor
            UserInfo userInfo = (UserInfo) request.getAttribute(interceptor.getATTRIBUTE_USER());
            if (userInfo == null) {
                log.error("No authenticated user found in request attributes");
                throw new RuntimeException("Authentication information not found");
            }
            return userInfo.getId();
        } catch (Exception e) {
            log.error("Failed to extract user ID from request: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to extract user information", e);
        }
    }
}
