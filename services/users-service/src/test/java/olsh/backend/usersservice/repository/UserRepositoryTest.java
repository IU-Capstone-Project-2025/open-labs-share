package olsh.backend.usersservice.repository;

import olsh.backend.usersservice.entity.Role;
import olsh.backend.usersservice.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("UserRepository Tests")
class UserRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("users_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Clear database before each test for isolation
        userRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();
        
        // Prepare minimal test data
        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .password("hashedPassword")
                .firstName("John")
                .lastName("Doe")
                .role(Role.ROLE_USER)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // === Standard JpaRepository Methods Tests ===

    @Test
    @DisplayName("Should save user and generate ID")
    void shouldSaveUserAndGenerateId() {
        // When
        User savedUser = userRepository.save(testUser);

        // Then
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getUsername()).isEqualTo("testuser");
        assertThat(savedUser.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Should find user by ID")
    void shouldFindUserById() {
        // Given
        User savedUser = entityManager.persistAndFlush(testUser);
        entityManager.clear();

        // When
        Optional<User> foundUser = userRepository.findById(savedUser.getId());

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("Should return empty when user not found by ID")
    void shouldReturnEmptyWhenUserNotFoundById() {
        // When
        Optional<User> foundUser = userRepository.findById(999L);

        // Then
        assertThat(foundUser).isEmpty();
    }

    @Test
    @DisplayName("Should find all users")
    void shouldFindAllUsers() {
        // Given
        User user2 = User.builder()
                .username("user2")
                .email("user2@example.com")
                .password("password2")
                .firstName("Jane")
                .lastName("Smith")
                .role(Role.ROLE_ADMIN)
                .build();
        
        entityManager.persist(testUser);
        entityManager.persist(user2);
        entityManager.flush();

        // When
        List<User> users = userRepository.findAll();

        // Then
        assertThat(users).hasSize(2);
        assertThat(users).extracting(User::getUsername)
                .containsExactlyInAnyOrder("testuser", "user2");
    }

    @Test
    @DisplayName("Should count users correctly")
    void shouldCountUsersCorrectly() {
        // Given
        entityManager.persist(testUser);
        entityManager.flush();

        // When
        long count = userRepository.count();

        // Then
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("Should delete user by ID")
    void shouldDeleteUserById() {
        // Given
        User savedUser = entityManager.persistAndFlush(testUser);
        Long userId = savedUser.getId();

        // When
        userRepository.deleteById(userId);

        // Then
        Optional<User> deletedUser = userRepository.findById(userId);
        assertThat(deletedUser).isEmpty();
        assertThat(userRepository.count()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should check if user exists by ID")
    void shouldCheckIfUserExistsById() {
        // Given
        User savedUser = entityManager.persistAndFlush(testUser);

        // When & Then
        assertThat(userRepository.existsById(savedUser.getId())).isTrue();
        assertThat(userRepository.existsById(999L)).isFalse();
    }

    // === Custom Finder Methods Tests ===

    @Test
    @DisplayName("Should find user by username")
    void shouldFindUserByUsername() {
        // Given
        entityManager.persistAndFlush(testUser);

        // When
        Optional<User> foundUser = userRepository.findByUsername("testuser");

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Should return empty when username not found")
    void shouldReturnEmptyWhenUsernameNotFound() {
        // When
        Optional<User> foundUser = userRepository.findByUsername("nonexistent");

        // Then
        assertThat(foundUser).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"TESTUSER", "TestUser", "testUSER"})
    @DisplayName("Should verify username search is case-sensitive by default")
    void shouldHandleUsernameCase(String searchUsername) {
        // Given
        entityManager.persistAndFlush(testUser);

        // When - Note: This tests the actual behavior, not case-insensitive search
        Optional<User> foundUser = userRepository.findByUsername(searchUsername);

        // Then - findByUsername is case-sensitive by default
        assertThat(foundUser).isEmpty();
    }

    @Test
    @DisplayName("Should find user by email")
    void shouldFindUserByEmail() {
        // Given
        entityManager.persistAndFlush(testUser);

        // When
        Optional<User> foundUser = userRepository.findByEmail("test@example.com");

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("Should return empty when email not found")
    void shouldReturnEmptyWhenEmailNotFound() {
        // When
        Optional<User> foundUser = userRepository.findByEmail("nonexistent@example.com");

        // Then
        assertThat(foundUser).isEmpty();
    }

    @Test
    @DisplayName("Should check if username exists")
    void shouldCheckIfUsernameExists() {
        // Given
        entityManager.persistAndFlush(testUser);

        // When & Then
        assertThat(userRepository.existsByUsername("testuser")).isTrue();
        assertThat(userRepository.existsByUsername("nonexistent")).isFalse();
    }

    @Test
    @DisplayName("Should check if email exists")
    void shouldCheckIfEmailExists() {
        // Given
        entityManager.persistAndFlush(testUser);

        // When & Then
        assertThat(userRepository.existsByEmail("test@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("nonexistent@example.com")).isFalse();
    }

    // === Custom Query Method Tests ===

    @Test
    @DisplayName("Should find users by username containing query case-insensitive")
    void shouldFindUsersByUsernameContainingQueryCaseInsensitive() {
        // Given
        User user2 = User.builder()
                .username("adminuser")
                .email("admin@example.com")
                .password("password")
                .firstName("Admin")
                .lastName("User")
                .role(Role.ROLE_ADMIN)
                .build();
        
        entityManager.persist(testUser);
        entityManager.persist(user2);
        entityManager.flush();

        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<User> result = userRepository.findByUsernameOrNameContainingIgnoreCase("USER", pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).extracting(User::getUsername)
                .containsExactlyInAnyOrder("testuser", "adminuser");
    }

    @Test
    @DisplayName("Should find users by first name containing query case-insensitive")
    void shouldFindUsersByFirstNameContainingQueryCaseInsensitive() {
        // Given
        entityManager.persistAndFlush(testUser);
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<User> result = userRepository.findByUsernameOrNameContainingIgnoreCase("john", pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getFirstName()).isEqualTo("John");
    }

    @Test
    @DisplayName("Should find users by last name containing query case-insensitive")
    void shouldFindUsersByLastNameContainingQueryCaseInsensitive() {
        // Given
        entityManager.persistAndFlush(testUser);
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<User> result = userRepository.findByUsernameOrNameContainingIgnoreCase("doe", pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getLastName()).isEqualTo("Doe");
    }

    @ParameterizedTest
    @CsvSource({
        "test, testuser",
        "john, testuser", 
        "DOE, testuser",
        "User, testuser"
    })
    @DisplayName("Should find users by partial name match case-insensitive")
    void shouldFindUsersByPartialNameMatchCaseInsensitive(String query, String expectedUsername) {
        // Given
        entityManager.persistAndFlush(testUser);
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<User> result = userRepository.findByUsernameOrNameContainingIgnoreCase(query, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getUsername()).isEqualTo(expectedUsername);
    }

    @Test
    @DisplayName("Should return empty page when no matches found")
    void shouldReturnEmptyPageWhenNoMatchesFound() {
        // Given
        entityManager.persistAndFlush(testUser);
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<User> result = userRepository.findByUsernameOrNameContainingIgnoreCase("nomatch", pageable);

        // Then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should handle pagination correctly")
    void shouldHandlePaginationCorrectly() {
        // Given
        User user2 = User.builder()
                .username("user2")
                .email("user2@example.com")
                .password("password")
                .firstName("Jane")
                .lastName("Smith")
                .role(Role.ROLE_USER)
                .build();
        
        User user3 = User.builder()
                .username("user3")
                .email("user3@example.com")
                .password("password")
                .firstName("Bob")
                .lastName("Johnson")
                .role(Role.ROLE_USER)
                .build();

        entityManager.persist(testUser);
        entityManager.persist(user2);
        entityManager.persist(user3);
        entityManager.flush();

        Pageable pageable = PageRequest.of(0, 2); // First page, 2 items per page

        // When
        Page<User> result = userRepository.findByUsernameOrNameContainingIgnoreCase("user", pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.hasNext()).isTrue();
    }

    @Test
    @DisplayName("Should handle sorting correctly")
    void shouldHandleSortingCorrectly() {
        // Given
        User user2 = User.builder()
                .username("anotheruser")
                .email("another@example.com")
                .password("password")
                .firstName("Alice")
                .lastName("Brown")
                .role(Role.ROLE_USER)
                .build();

        entityManager.persist(testUser);
        entityManager.persist(user2);
        entityManager.flush();

        Pageable pageable = PageRequest.of(0, 10, Sort.by("username"));

        // When
        Page<User> result = userRepository.findByUsernameOrNameContainingIgnoreCase("user", pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getUsername()).isEqualTo("anotheruser");
        assertThat(result.getContent().get(1).getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("Should handle empty query string")
    void shouldHandleEmptyQueryString() {
        // Given
        entityManager.persistAndFlush(testUser);
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<User> result = userRepository.findByUsernameOrNameContainingIgnoreCase("", pageable);

        // Then
        assertThat(result.getContent()).hasSize(1); // Empty string matches all
    }

    @Test
    @DisplayName("Should handle null query string gracefully")
    void shouldHandleNullQueryStringGracefully() {
        // Given
        entityManager.persistAndFlush(testUser);
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<User> result = userRepository.findByUsernameOrNameContainingIgnoreCase(null, pageable);

        // Then
        assertThat(result.getContent()).isEmpty(); // null doesn't match anything
    }

    // === Edge Cases and Error Scenarios ===

    @Test
    @DisplayName("Should handle special characters in search query")
    void shouldHandleSpecialCharactersInSearchQuery() {
        // Given
        User userWithSpecialChars = User.builder()
                .username("user@123")
                .email("special@example.com")
                .password("password")
                .firstName("Special")
                .lastName("User")
                .role(Role.ROLE_USER)
                .build();
        
        entityManager.persistAndFlush(userWithSpecialChars);
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<User> result = userRepository.findByUsernameOrNameContainingIgnoreCase("@123", pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getUsername()).isEqualTo("user@123");
    }

    @Test
    @DisplayName("Should handle Unicode characters correctly")
    void shouldHandleUnicodeCharactersCorrectly() {
        // Given
        User unicodeUser = User.builder()
                .username("unicodeuser")
                .email("unicode@example.com")
                .password("password")
                .firstName("José")
                .lastName("Müller")
                .role(Role.ROLE_USER)
                .build();
        
        entityManager.persistAndFlush(unicodeUser);
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<User> result = userRepository.findByUsernameOrNameContainingIgnoreCase("José", pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getFirstName()).isEqualTo("José");
    }
} 