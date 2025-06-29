package olsh.backend.usersservice.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Role Enum Tests")
class RoleEnumTest {

    @Test
    @DisplayName("Should have exactly two role values")
    void shouldHaveExactlyTwoRoleValues() {
        // When
        Role[] roles = Role.values();

        // Then
        assertThat(roles).hasSize(2);
        assertThat(roles).containsExactlyInAnyOrder(Role.ROLE_USER, Role.ROLE_ADMIN);
    }

    @Test
    @DisplayName("Should have ROLE_USER enum value")
    void shouldHaveRoleUserEnumValue() {
        // When & Then
        assertThat(Role.ROLE_USER).isNotNull();
        assertThat(Role.ROLE_USER.name()).isEqualTo("ROLE_USER");
        assertThat(Role.ROLE_USER.toString()).isEqualTo("ROLE_USER");
    }

    @Test
    @DisplayName("Should have ROLE_ADMIN enum value")
    void shouldHaveRoleAdminEnumValue() {
        // When & Then
        assertThat(Role.ROLE_ADMIN).isNotNull();
        assertThat(Role.ROLE_ADMIN.name()).isEqualTo("ROLE_ADMIN");
        assertThat(Role.ROLE_ADMIN.toString()).isEqualTo("ROLE_ADMIN");
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    @DisplayName("Should convert each role to string correctly")
    void shouldConvertEachRoleToStringCorrectly(Role role) {
        // When
        String roleName = role.name();
        String roleString = role.toString();

        // Then
        assertThat(roleName).isNotNull();
        assertThat(roleString).isNotNull();
        assertThat(roleName).isEqualTo(roleString);
        assertThat(roleName).startsWith("ROLE_");
    }

    @Test
    @DisplayName("Should parse ROLE_USER from string using valueOf")
    void shouldParseRoleUserFromStringUsingValueOf() {
        // When
        Role role = Role.valueOf("ROLE_USER");

        // Then
        assertThat(role).isEqualTo(Role.ROLE_USER);
    }

    @Test
    @DisplayName("Should parse ROLE_ADMIN from string using valueOf")
    void shouldParseRoleAdminFromStringUsingValueOf() {
        // When
        Role role = Role.valueOf("ROLE_ADMIN");

        // Then
        assertThat(role).isEqualTo(Role.ROLE_ADMIN);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for invalid role string")
    void shouldThrowIllegalArgumentExceptionForInvalidRoleString() {
        // When & Then
        assertThatThrownBy(() -> Role.valueOf("INVALID_ROLE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No enum constant")
                .hasMessageContaining("INVALID_ROLE");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for null role string")
    void shouldThrowIllegalArgumentExceptionForNullRoleString() {
        // When & Then
        assertThatThrownBy(() -> Role.valueOf(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Should be compatible with string representation")
    void shouldBeCompatibleWithStringRepresentation() {
        // When & Then
        assertThat(Role.ROLE_USER.toString()).isEqualTo("ROLE_USER");
        assertThat(Role.ROLE_ADMIN.toString()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    @DisplayName("Should maintain enum equality and hashCode consistency")
    void shouldMaintainEnumEqualityAndHashCodeConsistency() {
        // Given
        Role role1 = Role.ROLE_USER;
        Role role2 = Role.valueOf("ROLE_USER");

        // When & Then
        assertThat(role1).isEqualTo(role2);
        assertThat(role1.hashCode()).isEqualTo(role2.hashCode());
        assertThat(role1 == role2).isTrue(); // Enum identity
    }

    @Test
    @DisplayName("Should have different enum instances for different roles")
    void shouldHaveDifferentEnumInstancesForDifferentRoles() {
        // When & Then
        assertThat(Role.ROLE_USER).isNotEqualTo(Role.ROLE_ADMIN);
        assertThat(Role.ROLE_USER.hashCode()).isNotEqualTo(Role.ROLE_ADMIN.hashCode());
        assertThat(Role.ROLE_USER == Role.ROLE_ADMIN).isFalse();
    }

    @Test
    @DisplayName("Should support ordinal values")
    void shouldSupportOrdinalValues() {
        // When & Then
        assertThat(Role.ROLE_USER.ordinal()).isEqualTo(0);
        assertThat(Role.ROLE_ADMIN.ordinal()).isEqualTo(1);
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    @DisplayName("Should be able to use each role in switch statements")
    void shouldBeAbleToUseEachRoleInSwitchStatements(Role role) {
        // When
        String description = switch (role) {
            case ROLE_USER -> "Regular user";
            case ROLE_ADMIN -> "Administrator";
        };

        // Then
        assertThat(description).isNotNull();
        if (role == Role.ROLE_USER) {
            assertThat(description).isEqualTo("Regular user");
        } else if (role == Role.ROLE_ADMIN) {
            assertThat(description).isEqualTo("Administrator");
        }
    }
} 