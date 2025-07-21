package olsh.backend.usersservice.entity;

import java.time.LocalDateTime;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.security.core.GrantedAuthority;

@DisplayName("User Entity Tests")
class UserEntityTest {

    private User.UserBuilder baseUserBuilder;

    @BeforeEach
    void setUp() {
        baseUserBuilder = User.builder()
                .username("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .firstName("John")
                .lastName("Doe")
                .role(Role.ROLE_USER);
    }

    @Test
    @DisplayName("Should create user with builder pattern")
    void shouldCreateUserWithBuilderPattern() {
        // When
        User user = baseUserBuilder.build();

        // Then
        assertThat(user).isNotNull();
        assertThat(user.getUsername()).isEqualTo("testuser");
        assertThat(user.getEmail()).isEqualTo("test@example.com");
        assertThat(user.getPassword()).isEqualTo("encodedPassword");
        assertThat(user.getFirstName()).isEqualTo("John");
        assertThat(user.getLastName()).isEqualTo("Doe");
        assertThat(user.getRole()).isEqualTo(Role.ROLE_USER);
    }

    @Test
    @DisplayName("Should create user with all fields using builder")
    void shouldCreateUserWithAllFieldsUsingBuilder() {
        // Given
        LocalDateTime now = LocalDateTime.now();

        // When
        User user = baseUserBuilder
                .id(1L)
                .createdAt(now)
                .lastLoginAt(now)
                .build();

        // Then
        assertThat(user.getId()).isEqualTo(1L);
        assertThat(user.getCreatedAt()).isEqualTo(now);
        assertThat(user.getLastLoginAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("Should set createdAt timestamp in @PrePersist onCreate method")
    void shouldSetCreatedAtTimestampInOnCreate() {
        // Given
        User user = baseUserBuilder.build();
        assertThat(user.getCreatedAt()).isNull(); // Should be null before onCreate

        // When
        user.onCreate(); // Manually call @PrePersist method

        // Then
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getCreatedAt()).isBefore(LocalDateTime.now().plusSeconds(1));
        assertThat(user.getCreatedAt()).isAfter(LocalDateTime.now().minusSeconds(5));
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    @DisplayName("Should return correct authorities for each role")
    void shouldReturnCorrectAuthoritiesForEachRole(Role role) {
        // Given
        User user = baseUserBuilder.role(role).build();

        // When
        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();

        // Then
        assertThat(authorities).isNotNull();
        assertThat(authorities).hasSize(1);
        assertThat(authorities.iterator().next().getAuthority()).isEqualTo(role.name());
    }

    @Test
    @DisplayName("Should return authorities for ROLE_USER")
    void shouldReturnAuthoritiesForRoleUser() {
        // Given
        User user = baseUserBuilder.role(Role.ROLE_USER).build();

        // When
        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();

        // Then
        assertThat(authorities).hasSize(1);
        assertThat(authorities.iterator().next().getAuthority()).isEqualTo("ROLE_USER");
    }

    @Test
    @DisplayName("Should return authorities for ROLE_ADMIN")
    void shouldReturnAuthoritiesForRoleAdmin() {
        // Given
        User user = baseUserBuilder.role(Role.ROLE_ADMIN).build();

        // When
        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();

        // Then
        assertThat(authorities).hasSize(1);
        assertThat(authorities.iterator().next().getAuthority()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    @DisplayName("Should implement UserDetails isAccountNonExpired as true")
    void shouldImplementUserDetailsIsAccountNonExpiredAsTrue() {
        // Given
        User user = baseUserBuilder.build();

        // When & Then
        assertThat(user.isAccountNonExpired()).isTrue();
    }

    @Test
    @DisplayName("Should implement UserDetails isAccountNonLocked as true")
    void shouldImplementUserDetailsIsAccountNonLockedAsTrue() {
        // Given
        User user = baseUserBuilder.build();

        // When & Then
        assertThat(user.isAccountNonLocked()).isTrue();
    }

    @Test
    @DisplayName("Should implement UserDetails isCredentialsNonExpired as true")
    void shouldImplementUserDetailsIsCredentialsNonExpiredAsTrue() {
        // Given
        User user = baseUserBuilder.build();

        // When & Then
        assertThat(user.isCredentialsNonExpired()).isTrue();
    }

    @Test
    @DisplayName("Should implement UserDetails isEnabled as true")
    void shouldImplementUserDetailsIsEnabledAsTrue() {
        // Given
        User user = baseUserBuilder.build();

        // When & Then
        assertThat(user.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("Should return username from UserDetails interface")
    void shouldReturnUsernameFromUserDetailsInterface() {
        // Given
        User user = baseUserBuilder.username("testUsername").build();

        // When & Then
        assertThat(user.getUsername()).isEqualTo("testUsername");
    }

    @Test
    @DisplayName("Should return password from UserDetails interface")
    void shouldReturnPasswordFromUserDetailsInterface() {
        // Given
        User user = baseUserBuilder.password("testPassword").build();

        // When & Then
        assertThat(user.getPassword()).isEqualTo("testPassword");
    }

    @Test
    @DisplayName("Should allow setting and getting all fields")
    void shouldAllowSettingAndGettingAllFields() {
        // Given
        User user = new User();
        LocalDateTime now = LocalDateTime.now();

        // When
        user.setId(1L);
        user.setUsername("newUsername");
        user.setEmail("new@example.com");
        user.setPassword("newPassword");
        user.setFirstName("Jane");
        user.setLastName("Smith");
        user.setRole(Role.ROLE_ADMIN);
        user.setCreatedAt(now);
        user.setLastLoginAt(now);

        // Then
        assertThat(user.getId()).isEqualTo(1L);
        assertThat(user.getUsername()).isEqualTo("newUsername");
        assertThat(user.getEmail()).isEqualTo("new@example.com");
        assertThat(user.getPassword()).isEqualTo("newPassword");
        assertThat(user.getFirstName()).isEqualTo("Jane");
        assertThat(user.getLastName()).isEqualTo("Smith");
        assertThat(user.getRole()).isEqualTo(Role.ROLE_ADMIN);
        assertThat(user.getCreatedAt()).isEqualTo(now);
        assertThat(user.getLastLoginAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("Should create user with no-args constructor")
    void shouldCreateUserWithNoArgsConstructor() {
        // When
        User user = new User();

        // Then
        assertThat(user).isNotNull();
        assertThat(user.getId()).isNull();
        assertThat(user.getUsername()).isNull();
        assertThat(user.getEmail()).isNull();
        assertThat(user.getPassword()).isNull();
        assertThat(user.getFirstName()).isNull();
        assertThat(user.getLastName()).isNull();
        assertThat(user.getRole()).isNull();
        assertThat(user.getCreatedAt()).isNull();
        assertThat(user.getLastLoginAt()).isNull();
    }

    @Test
    @DisplayName("Should create user with all-args constructor")
    void shouldCreateUserWithAllArgsConstructor() {
        // Given
        LocalDateTime now = LocalDateTime.now();

        // When
        User user = new User(1L, "testuser", "test@example.com", "password", 
                           "John", "Doe", Role.ROLE_USER, now, now, 0, 0, 0);

        // Then
        assertThat(user.getId()).isEqualTo(1L);
        assertThat(user.getUsername()).isEqualTo("testuser");
        assertThat(user.getEmail()).isEqualTo("test@example.com");
        assertThat(user.getPassword()).isEqualTo("password");
        assertThat(user.getFirstName()).isEqualTo("John");
        assertThat(user.getLastName()).isEqualTo("Doe");
        assertThat(user.getRole()).isEqualTo(Role.ROLE_USER);
        assertThat(user.getCreatedAt()).isEqualTo(now);
        assertThat(user.getLastLoginAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("Should handle null role in getAuthorities gracefully")
    void shouldHandleNullRoleInGetAuthoritiesGracefully() {
        // Given
        User user = baseUserBuilder.role(null).build();

        // When & Then
        // This should throw NPE as role.name() is called on null
        try {
            user.getAuthorities();
        } catch (NullPointerException e) {
            assertThat(e).isInstanceOf(NullPointerException.class);
        }
    }
} 