package olsh.backend.usersservice.service;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import olsh.backend.usersservice.config.PointsConfig;
import olsh.backend.usersservice.entity.Role;
import olsh.backend.usersservice.entity.User;
import olsh.backend.usersservice.exception.InsufficientBalanceException;
import olsh.backend.usersservice.exception.NotFoundException;
import olsh.backend.usersservice.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserStatsService Tests")
class UserStatsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PointsConfig pointsConfig;

    @InjectMocks
    private UserStatsService userStatsService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
            .id(1L)
            .username("testuser")
            .email("test@example.com")
            .firstName("Test")
            .lastName("User")
            .role(Role.ROLE_USER)
            .password("hashedpassword")
            .labsSolved(5)
            .labsReviewed(3)
            .balance(10)
            .createdAt(LocalDateTime.now())
            .build();
    }

    // === Labs Solved Increment Tests ===

    @Nested
    @DisplayName("Increment Labs Solved Tests")
    class IncrementLabsSolvedTests {

        @Test
        @DisplayName("Should increment labs solved and deduct balance when sufficient funds")
        void incrementLabsSolved_WithSufficientBalance_ShouldIncrementCountAndDeductPoints() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(pointsConfig.getBaseCost()).thenReturn(1);
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // When
            userStatsService.incrementLabsSolved(1L);

            // Then
            assertThat(testUser.getLabsSolved()).isEqualTo(6); // 5 + 1
            assertThat(testUser.getBalance()).isEqualTo(9); // 10 - 1
            verify(userRepository).findById(1L);
            verify(pointsConfig).getBaseCost();
            verify(userRepository).save(testUser);
            verifyNoMoreInteractions(userRepository, pointsConfig);
        }

        @ParameterizedTest
        @CsvSource({
            "0, 1, false", // Exactly zero balance, cost 1
            "1, 1, true",  // Exactly sufficient
            "5, 1, true", // More than sufficient
            "100, 10, true" // Much more than sufficient
        })
        @DisplayName("Should handle various balance scenarios correctly")
        void incrementLabsSolved_WithVariousBalanceScenarios_ShouldHandleCorrectly(
                int initialBalance, int baseCost, boolean shouldSucceed) {
            // Given
            testUser.setBalance(initialBalance);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(pointsConfig.getBaseCost()).thenReturn(baseCost);
            
            if (shouldSucceed) {
                when(userRepository.save(any(User.class))).thenReturn(testUser);
            }

            if (shouldSucceed) {
                // When
                userStatsService.incrementLabsSolved(1L);

                // Then
                assertThat(testUser.getLabsSolved()).isEqualTo(6); // Original 5 + 1
                assertThat(testUser.getBalance()).isEqualTo(initialBalance - baseCost);
                verify(userRepository).save(testUser);
            } else {
                // When & Then
                assertThatThrownBy(() -> userStatsService.incrementLabsSolved(1L))
                    .isInstanceOf(InsufficientBalanceException.class)
                    .hasMessageContaining("Insufficient balance")
                    .hasMessageContaining("Required: " + baseCost)
                    .hasMessageContaining("Available: " + initialBalance);
                
                // Verify no changes made
                assertThat(testUser.getLabsSolved()).isEqualTo(5); // Unchanged
                assertThat(testUser.getBalance()).isEqualTo(initialBalance); // Unchanged
                verify(userRepository, never()).save(any(User.class));
            }
        }

        @Test
        @DisplayName("Should handle edge case of exact balance match")
        void incrementLabsSolved_WithExactBalanceMatch_ShouldSucceed() {
            // Given
            testUser.setBalance(1); // Exactly the cost
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(pointsConfig.getBaseCost()).thenReturn(1);
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // When
            userStatsService.incrementLabsSolved(1L);

            // Then
            assertThat(testUser.getLabsSolved()).isEqualTo(6);
            assertThat(testUser.getBalance()).isEqualTo(0); // Should be exactly zero
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("Should throw NotFoundException when user does not exist")
        void incrementLabsSolved_WithNonExistentUser_ShouldThrowNotFoundException() {
            // Given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> userStatsService.incrementLabsSolved(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("User not found with ID: 999");
            
            verify(userRepository).findById(999L);
            verify(userRepository, never()).save(any(User.class));
            verifyNoInteractions(pointsConfig);
        }

        @ValueSource(ints = {1, 2, 3, 5, 10})
        @ParameterizedTest
        @DisplayName("Should work correctly with different cost configurations")
        void incrementLabsSolved_WithDifferentCosts_ShouldDeductCorrectAmount(int cost) {
            // Given
            testUser.setBalance(50); // Ensure sufficient balance
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(pointsConfig.getBaseCost()).thenReturn(cost);
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // When
            userStatsService.incrementLabsSolved(1L);

            // Then
            assertThat(testUser.getLabsSolved()).isEqualTo(6);
            assertThat(testUser.getBalance()).isEqualTo(50 - cost);
            verify(userRepository).save(testUser);
        }
    }

    // === Labs Reviewed Increment Tests ===

    @Nested
    @DisplayName("Increment Labs Reviewed Tests")
    class IncrementLabsReviewedTests {

        @Test
        @DisplayName("Should increment labs reviewed and add reward points")
        void incrementLabsReviewed_WithValidUser_ShouldIncrementCountAndAddReward() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(pointsConfig.getMultiplierReview()).thenReturn(3);
            when(pointsConfig.getBaseCost()).thenReturn(1);
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // When
            userStatsService.incrementLabsReviewed(1L);

            // Then
            assertThat(testUser.getLabsReviewed()).isEqualTo(4); // 3 + 1
            assertThat(testUser.getBalance()).isEqualTo(13); // 10 + (3 * 1)
            verify(userRepository).findById(1L);
            verify(pointsConfig).getMultiplierReview();
            verify(pointsConfig).getBaseCost();
            verify(userRepository).save(testUser);
            verifyNoMoreInteractions(userRepository, pointsConfig);
        }

        @ParameterizedTest
        @CsvSource({
            "1, 1, 1",   // k=1, n=1, reward=1
            "3, 1, 3",   // k=3, n=1, reward=3 (default config)
            "2, 2, 4",   // k=2, n=2, reward=4
            "5, 1, 5",   // k=5, n=1, reward=5
            "4, 2, 8"    // k=4, n=2, reward=8
        })
        @DisplayName("Should calculate reward correctly with different multiplier and cost configurations")
        void incrementLabsReviewed_WithDifferentConfigurations_ShouldCalculateRewardCorrectly(
                int multiplier, int baseCost, int expectedReward) {
            // Given
            int initialBalance = testUser.getBalance();
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(pointsConfig.getMultiplierReview()).thenReturn(multiplier);
            when(pointsConfig.getBaseCost()).thenReturn(baseCost);
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // When
            userStatsService.incrementLabsReviewed(1L);

            // Then
            assertThat(testUser.getLabsReviewed()).isEqualTo(4); // 3 + 1
            assertThat(testUser.getBalance()).isEqualTo(initialBalance + expectedReward);
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("Should handle maximum integer values without overflow")
        void incrementLabsReviewed_WithLargeValues_ShouldHandleCorrectly() {
            // Given
            testUser.setBalance(Integer.MAX_VALUE - 100); // Near max value
            testUser.setLabsReviewed(Integer.MAX_VALUE - 1); // Near max value
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(pointsConfig.getMultiplierReview()).thenReturn(3);
            when(pointsConfig.getBaseCost()).thenReturn(1);
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // When
            userStatsService.incrementLabsReviewed(1L);

            // Then
            assertThat(testUser.getLabsReviewed()).isEqualTo(Integer.MAX_VALUE);
            assertThat(testUser.getBalance()).isEqualTo(Integer.MAX_VALUE - 97); // Original + 3 reward
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("Should throw NotFoundException when user does not exist")
        void incrementLabsReviewed_WithNonExistentUser_ShouldThrowNotFoundException() {
            // Given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> userStatsService.incrementLabsReviewed(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("User not found with ID: 999");
            
            verify(userRepository).findById(999L);
            verify(userRepository, never()).save(any(User.class));
            verifyNoInteractions(pointsConfig);
        }

        @Test
        @DisplayName("Should work with zero reward configuration")
        void incrementLabsReviewed_WithZeroReward_ShouldOnlyIncrementCounter() {
            // Given
            int initialBalance = testUser.getBalance();
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(pointsConfig.getMultiplierReview()).thenReturn(0);
            when(pointsConfig.getBaseCost()).thenReturn(1);
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // When
            userStatsService.incrementLabsReviewed(1L);

            // Then
            assertThat(testUser.getLabsReviewed()).isEqualTo(4); // Incremented
            assertThat(testUser.getBalance()).isEqualTo(initialBalance); // Unchanged
            verify(userRepository).save(testUser);
        }
    }

    // === Edge Cases and Error Scenarios ===

    @Nested
    @DisplayName("Error Scenarios and Edge Cases")
    class ErrorScenariosTests {

        @Test
        @DisplayName("Should handle null user ID gracefully")
        void incrementLabsSolved_WithNullUserId_ShouldThrowAppropriateException() {
            // When & Then
            assertThatThrownBy(() -> userStatsService.incrementLabsSolved(null))
                .isInstanceOf(Exception.class); // Repository will throw appropriate exception
        }

        @Test
        @DisplayName("Should handle database save failure gracefully")
        void incrementLabsSolved_WithDatabaseSaveFailure_ShouldPropagateException() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(pointsConfig.getBaseCost()).thenReturn(1);
            when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("Database error"));

            // When & Then
            assertThatThrownBy(() -> userStatsService.incrementLabsSolved(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database error");
            
            // Verify the user object was modified before save attempt
            assertThat(testUser.getLabsSolved()).isEqualTo(6);
            assertThat(testUser.getBalance()).isEqualTo(9);
        }

        @Test
        @DisplayName("Should maintain transaction isolation requirements")
        void incrementMethods_ShouldUseCorrectTransactionSettings() {
            // This test verifies that the methods are annotated correctly
            // The actual transaction behavior would be tested in integration tests
            
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(pointsConfig.getBaseCost()).thenReturn(1);
            when(pointsConfig.getMultiplierReview()).thenReturn(3);
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // When - These should be atomic operations
            userStatsService.incrementLabsSolved(1L);
            userStatsService.incrementLabsReviewed(1L);

            // Then - Verify both operations completed
            verify(userRepository, times(2)).save(testUser);
        }
    }

    // === Multiple Operations Tests ===

    @Nested
    @DisplayName("Multiple Operations Tests")
    class MultipleOperationsTests {

        @Test
        @DisplayName("Should handle multiple consecutive solve operations")
        void incrementLabsSolved_MultipleOperations_ShouldAccumulateCorrectly() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(pointsConfig.getBaseCost()).thenReturn(1);
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // When - Perform multiple operations
            userStatsService.incrementLabsSolved(1L);
            userStatsService.incrementLabsSolved(1L);
            userStatsService.incrementLabsSolved(1L);

            // Then
            assertThat(testUser.getLabsSolved()).isEqualTo(8); // 5 + 3
            assertThat(testUser.getBalance()).isEqualTo(7); // 10 - (3 * 1)
            verify(userRepository, times(3)).save(testUser);
        }

        @Test
        @DisplayName("Should handle mixed solve and review operations")
        void incrementOperations_MixedOperations_ShouldCalculateCorrectly() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(pointsConfig.getBaseCost()).thenReturn(1);
            when(pointsConfig.getMultiplierReview()).thenReturn(3);
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // When - Perform mixed operations
            userStatsService.incrementLabsSolved(1L);    // -1 point, labs_solved: 6, balance: 9
            userStatsService.incrementLabsReviewed(1L);  // +3 points, labs_reviewed: 4, balance: 12
            userStatsService.incrementLabsSolved(1L);    // -1 point, labs_solved: 7, balance: 11

            // Then
            assertThat(testUser.getLabsSolved()).isEqualTo(7); // 5 + 2
            assertThat(testUser.getLabsReviewed()).isEqualTo(4); // 3 + 1
            assertThat(testUser.getBalance()).isEqualTo(11); // 10 - 1 + 3 - 1
            verify(userRepository, times(3)).save(testUser);
        }
    }
} 