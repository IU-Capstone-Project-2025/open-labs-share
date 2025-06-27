package olsh.backend.api_gateway.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.Documented;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@Target(ElementType.METHOD) // Applicable to methods only
@Retention(RetentionPolicy.RUNTIME) // Used at runtime
@SecurityRequirement(name = "bearerAuth")
@Documented
public @interface RequireAuth {
    /**
     * Set the user roles required for access.
     * If empty, any authenticated user can access.
     */
    String[] roles() default {};

    /**
     * Custom error message
     */
    String message() default "Authentication required";
}
