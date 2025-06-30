package olsh.backend.usersservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties(prefix = "points")
@Getter
@Setter
public class PointsConfig {
    
    /**
     * Multiplier for review rewards (k)
     * User gets k * base-cost points for reviewing
     */
    private int multiplierReview = 3;
    
    /**
     * Base points cost/reward (n)
     * User pays n points to solve a lab
     * Base reward amount that gets multiplied for reviews
     */
    private int baseCost = 1;
    
    /**
     * Initial balance for new users (m)
     * Starting points when user registers
     */
    private int initialBalance = 10;
} 