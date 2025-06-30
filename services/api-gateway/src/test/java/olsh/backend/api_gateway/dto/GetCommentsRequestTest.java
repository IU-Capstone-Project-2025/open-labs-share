package olsh.backend.api_gateway.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import olsh.backend.api_gateway.dto.request.GetCommentsRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GetCommentsRequest - DTO Validation Tests")
class GetCommentsRequestTest {

    private Validator validator;
    private ValidatorFactory validatorFactory;

    @BeforeEach
    @DisplayName("Set up Jakarta Bean Validation validator")
    void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
        System.out.println("Setting up GetCommentsRequest validation tests...");
    }

    @AfterEach
    @DisplayName("Clean up validation factory")
    void tearDown() {
        if (validatorFactory != null) {
            validatorFactory.close();
        }
        System.out.println("Cleaning up GetCommentsRequest validation tests...");
    }

    // Test 1: Valid requests with default values should pass
    @Test
    @DisplayName("Should pass validation with default values (page=1, limit=20)")
    void getCommentsRequest_DefaultValues_PassesValidation() {
        // Given
        GetCommentsRequest request = new GetCommentsRequest();

        // When
        Set<ConstraintViolation<GetCommentsRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
        assertThat(request.getPage()).isEqualTo(1);
        assertThat(request.getLimit()).isEqualTo(20);
    }

    @Test
    @DisplayName("Should pass validation with custom valid values")
    void getCommentsRequest_CustomValidValues_PassesValidation() {
        // Given
        GetCommentsRequest request = new GetCommentsRequest(5, 50);

        // When
        Set<ConstraintViolation<GetCommentsRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
        assertThat(request.getPage()).isEqualTo(5);
        assertThat(request.getLimit()).isEqualTo(50);
    }

    // Test 2: Page validation tests
    @Test
    @DisplayName("Should fail validation when page is 0")
    void getCommentsRequest_PageZero_FailsValidation() {
        // Given
        GetCommentsRequest request = new GetCommentsRequest(0, 20);

        // When
        Set<ConstraintViolation<GetCommentsRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .hasSize(1)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("Page number must be at least 1");
    }

    @ParameterizedTest(name = "Should fail validation when page is {0}")
    @ValueSource(ints = {-100, -10, -1, 0})
    @DisplayName("Page validation - Invalid page numbers")
    void getCommentsRequest_InvalidPageNumbers_FailsValidation(int invalidPage) {
        // Given
        GetCommentsRequest request = new GetCommentsRequest(invalidPage, 20);

        // When
        Set<ConstraintViolation<GetCommentsRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .hasSize(1)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("Page number must be at least 1");
    }

    @ParameterizedTest(name = "Should pass validation when page is {0}")
    @ValueSource(ints = {1, 2, 10, 100, 1000, Integer.MAX_VALUE})
    @DisplayName("Page validation - Valid page numbers")
    void getCommentsRequest_ValidPageNumbers_PassesValidation(int validPage) {
        // Given
        GetCommentsRequest request = new GetCommentsRequest(validPage, 20);

        // When
        Set<ConstraintViolation<GetCommentsRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
    }

    // Test 3: Limit validation tests
    @Test
    @DisplayName("Should fail validation when limit is 0")
    void getCommentsRequest_LimitZero_FailsValidation() {
        // Given
        GetCommentsRequest request = new GetCommentsRequest(1, 0);

        // When
        Set<ConstraintViolation<GetCommentsRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .hasSize(1)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("Limit must be at least 1");
    }

    @ParameterizedTest(name = "Should fail validation when limit is {0} (below minimum)")
    @ValueSource(ints = {-100, -10, -1, 0})
    @DisplayName("Limit validation - Below minimum")
    void getCommentsRequest_LimitBelowMinimum_FailsValidation(int invalidLimit) {
        // Given
        GetCommentsRequest request = new GetCommentsRequest(1, invalidLimit);

        // When
        Set<ConstraintViolation<GetCommentsRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .hasSize(1)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("Limit must be at least 1");
    }

    @Test
    @DisplayName("Should fail validation when limit exceeds maximum (101)")
    void getCommentsRequest_LimitExceedsMaximum_FailsValidation() {
        // Given
        GetCommentsRequest request = new GetCommentsRequest(1, 101);

        // When
        Set<ConstraintViolation<GetCommentsRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .hasSize(1)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("Limit cannot exceed 100");
    }

    @ParameterizedTest(name = "Should fail validation when limit is {0} (above maximum)")
    @ValueSource(ints = {101, 150, 200, 1000, Integer.MAX_VALUE})
    @DisplayName("Limit validation - Above maximum")
    void getCommentsRequest_LimitAboveMaximum_FailsValidation(int invalidLimit) {
        // Given
        GetCommentsRequest request = new GetCommentsRequest(1, invalidLimit);

        // When
        Set<ConstraintViolation<GetCommentsRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .hasSize(1)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("Limit cannot exceed 100");
    }

    @ParameterizedTest(name = "Should pass validation when limit is {0}")
    @ValueSource(ints = {1, 10, 20, 50, 99, 100})
    @DisplayName("Limit validation - Valid limits")
    void getCommentsRequest_ValidLimits_PassesValidation(int validLimit) {
        // Given
        GetCommentsRequest request = new GetCommentsRequest(1, validLimit);

        // When
        Set<ConstraintViolation<GetCommentsRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
    }

    // Test 4: Boundary value testing
    @Test
    @DisplayName("Should pass validation at minimum boundaries (page=1, limit=1)")
    void getCommentsRequest_MinimumBoundaries_PassesValidation() {
        // Given
        GetCommentsRequest request = new GetCommentsRequest(1, 1);

        // When
        Set<ConstraintViolation<GetCommentsRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should pass validation at maximum boundary for limit (page=1, limit=100)")
    void getCommentsRequest_MaximumLimitBoundary_PassesValidation() {
        // Given
        GetCommentsRequest request = new GetCommentsRequest(1, 100);

        // When
        Set<ConstraintViolation<GetCommentsRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
    }

    // Test 5: Multiple validation errors
    @Test
    @DisplayName("Should return multiple validation errors when both fields are invalid")
    void getCommentsRequest_BothFieldsInvalid_ReturnsMultipleErrors() {
        // Given
        GetCommentsRequest request = new GetCommentsRequest(0, 101); // Both invalid

        // When
        Set<ConstraintViolation<GetCommentsRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(2);
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactlyInAnyOrder(
                        "Page number must be at least 1",
                        "Limit cannot exceed 100"
                );
    }

    @Test
    @DisplayName("Should return multiple errors when both fields violate minimum constraints")
    void getCommentsRequest_BothFieldsBelowMinimum_ReturnsMultipleErrors() {
        // Given
        GetCommentsRequest request = new GetCommentsRequest(-1, -5);

        // When
        Set<ConstraintViolation<GetCommentsRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(2);
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactlyInAnyOrder(
                        "Page number must be at least 1",
                        "Limit must be at least 1"
                );
    }

    // Test 6: Field name verification
    @Test
    @DisplayName("Should identify correct field names in constraint violations")
    void getCommentsRequest_FieldNameVerification_ContainsCorrectFieldNames() {
        // Given
        GetCommentsRequest request = new GetCommentsRequest(0, 101);

        // When
        Set<ConstraintViolation<GetCommentsRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactlyInAnyOrder("page", "limit");
    }

    // Test 7: Constructor and accessor testing
    @Test
    @DisplayName("Should correctly set and get values through constructors and accessors")
    void getCommentsRequest_ConstructorsAndAccessors_WorkCorrectly() {
        // Test all-args constructor
        GetCommentsRequest request1 = new GetCommentsRequest(10, 50);
        assertThat(request1.getPage()).isEqualTo(10);
        assertThat(request1.getLimit()).isEqualTo(50);

        // Test no-args constructor + setters
        GetCommentsRequest request2 = new GetCommentsRequest();
        request2.setPage(5);
        request2.setLimit(25);
        assertThat(request2.getPage()).isEqualTo(5);
        assertThat(request2.getLimit()).isEqualTo(25);

        // Verify both pass validation
        assertThat(validator.validate(request1)).isEmpty();
        assertThat(validator.validate(request2)).isEmpty();
    }

    // Test 8: Null value handling (if using Integer wrapper)
    @Test
    @DisplayName("Should handle null values gracefully")
    void getCommentsRequest_NullValues_HandledCorrectly() {
        // Given
        GetCommentsRequest request = new GetCommentsRequest();
        request.setPage(null);
        request.setLimit(null);

        // When
        Set<ConstraintViolation<GetCommentsRequest>> violations = validator.validate(request);

        // Then - Null values should pass validation (using Integer wrapper)
        assertThat(violations).isEmpty();
    }

    // Test 9: Real-world pagination scenarios
    @Test
    @DisplayName("Should handle common pagination scenarios correctly")
    void getCommentsRequest_CommonPaginationScenarios_WorkCorrectly() {
        // Test common scenarios
        GetCommentsRequest[] scenarios = {
                new GetCommentsRequest(1, 10),   // First page, small size
                new GetCommentsRequest(1, 20),   // Default scenario
                new GetCommentsRequest(5, 50),   // Mid-range pagination
                new GetCommentsRequest(100, 100) // Large page, max size
        };

        for (GetCommentsRequest scenario : scenarios) {
            Set<ConstraintViolation<GetCommentsRequest>> violations = validator.validate(scenario);
            assertThat(violations)
                    .as("Scenario page=%d, limit=%d should be valid", scenario.getPage(), scenario.getLimit())
                    .isEmpty();
        }
    }

    // Test 10: Error message consistency
    @Test
    @DisplayName("Should provide consistent error messages for the same validation failures")
    void getCommentsRequest_ErrorMessageConsistency_IsConsistent() {
        // Test multiple instances with same invalid values
        GetCommentsRequest[] invalidRequests = {
                new GetCommentsRequest(0, 20),
                new GetCommentsRequest(-1, 20),
                new GetCommentsRequest(-100, 20)
        };

        String expectedPageMessage = "Page number must be at least 1";

        for (GetCommentsRequest request : invalidRequests) {
            Set<ConstraintViolation<GetCommentsRequest>> violations = validator.validate(request);
            assertThat(violations)
                    .hasSize(1)
                    .extracting(ConstraintViolation::getMessage)
                    .containsExactly(expectedPageMessage);
        }
    }
}

