package olsh.backend.usersservice.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import olsh.backend.usersservice.config.PointsConfig;
import olsh.backend.usersservice.entity.User;
import olsh.backend.usersservice.exception.InsufficientBalanceException;
import olsh.backend.usersservice.exception.NotFoundException;
import olsh.backend.usersservice.repository.UserRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserStatsService {

    private final UserRepository userRepository;
    private final PointsConfig pointsConfig;

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void incrementLabsSolved(Long userId) {
        log.debug("Incrementing labs solved for user ID: {}", userId);
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User not found with ID: " + userId));
        
        int baseCost = pointsConfig.getBaseCost();
        
        // Check if user has sufficient balance
        if (user.getBalance() < baseCost) {
            log.warn("Insufficient balance for user ID: {}. Required: {}, Available: {}", 
                     userId, baseCost, user.getBalance());
            throw new InsufficientBalanceException(
                String.format("Insufficient balance. Required: %d points, Available: %d points", 
                             baseCost, user.getBalance()));
        }
        
        // Increment counter and deduct points atomically
        user.setLabsSolved(user.getLabsSolved() + 1);
        user.setBalance(user.getBalance() - baseCost);
        
        userRepository.save(user);
        
        log.info("Successfully incremented labs solved for user ID: {}. New count: {}, New balance: {}", 
                 userId, user.getLabsSolved(), user.getBalance());
    }
    
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void incrementLabsReviewed(Long userId) {
        log.debug("Incrementing labs reviewed for user ID: {}", userId);
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User not found with ID: " + userId));
        
        int rewardPoints = pointsConfig.getMultiplierReview() * pointsConfig.getBaseCost();
        
        // Increment counter and add reward points atomically
        user.setLabsReviewed(user.getLabsReviewed() + 1);
        user.setBalance(user.getBalance() + rewardPoints);
        
        userRepository.save(user);
        
        log.info("Successfully incremented labs reviewed for user ID: {}. New count: {}, New balance: {}, Reward: {} points", 
                 userId, user.getLabsReviewed(), user.getBalance(), rewardPoints);
    }
} 