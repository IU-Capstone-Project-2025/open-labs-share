package olsh.backend.api_gateway.dto;


import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import olsh.backend.api_gateway.dto.request.GetLabsRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class GetLabsRequestTest {

    private Validator validator;
    private ValidatorFactory validatorFactory;

    @BeforeEach
    void setUp() {
        // Create Jakarta Bean Validation validator
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
        System.out.println("Setting up GetLabsRequest validation tests...");
    }

    @AfterEach
    void tearDown() {
        if (validatorFactory != null) {
            validatorFactory.close();
        }
        System.out.println("Cleaning up GetLabsRequest validation tests...");
    }

    // Test 1: Valid DTO with default values should pass validation
    @Test
    void getLabsRequest_DefaultValues_PassesValidation() {
        // Given - Using default constructor (should use default values)
        GetLabsRequest request = new GetLabsRequest();

        // When
        Set<ConstraintViolation<GetLabsRequest>> violations = validator.validate(request);

        // Then - Default values should be valid
        assertThat(violations).isEmpty();
        assertThat(request.getPage()).isEqualTo(1);
        assertThat(request.getLimit()).isEqualTo(20);
    }

    @Test
    void getLabsRequest_ValidCustomValues_PassesValidation() {
        // Given - Create a valid DTO with custom values
        GetLabsRequest request = createValidRequest(5, 50);

        // When
        Set<ConstraintViolation<GetLabsRequest>> violations = validator.validate(request);

        // Then - Should have no validation errors
        assertThat(violations).isEmpty();
    }

    @Test
    void getLabsRequest_ValidBoundaryValues_PassesValidation() {
        // Given - Test boundary values (minimum and maximum)
        GetLabsRequest request = createValidRequest(1, 100);

        // When
        Set<ConstraintViolation<GetLabsRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
    }

    // Test 2: Page validation tests
    @Test
    void getLabsRequest_ZeroPage_FailsValidation() {
        // Given
        GetLabsRequest request = createValidRequest(0, 20); // Invalid page

        // When
        Set<ConstraintViolation<GetLabsRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .hasSize(1)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("Page number must be at least 1");

        // Check the field name
        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("page");
    }

    @Test
    void getLabsRequest_NegativePage_FailsValidation() {
        // Given
        GetLabsRequest request = createValidRequest(-1, 20); // Invalid page

        // When
        Set<ConstraintViolation<GetLabsRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .hasSize(1)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("Page number must be at least 1");
    }

    @Test
    void getLabsRequest_PageOne_PassesValidation() {
        // Given - Testing minimum boundary condition
        GetLabsRequest request = createValidRequest(1, 20);

        // When
        Set<ConstraintViolation<GetLabsRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    void getLabsRequest_VeryLargePage_PassesValidation() {
        // Given - Testing that there's no upper limit on page
        GetLabsRequest request = createValidRequest(Integer.MAX_VALUE, 20);

        // When
        Set<ConstraintViolation<GetLabsRequest>> violations = validator.validate(request);

        // Then - Should pass (no max constraint on page)
        assertThat(violations).isEmpty();
    }

    // Test 3: Limit validation tests
    @Test
    void getLabsRequest_ZeroLimit_FailsValidation() {
        // Given
        GetLabsRequest request = createValidRequest(1, 0); // Invalid limit

        // When
        Set<ConstraintViolation<GetLabsRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .hasSize(1)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("Limit must be at least 1");
    }

    @Test
    void getLabsRequest_NegativeLimit_FailsValidation() {
        // Given
        GetLabsRequest request = createValidRequest(1, -5); // Invalid limit

        // When
        Set<ConstraintViolation<GetLabsRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .hasSize(1)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("Limit must be at least 1");
    }

    @Test
    void getLabsRequest_LimitExceedsMaximum_FailsValidation() {
        // Given
        GetLabsRequest request = createValidRequest(1, 101); // Invalid - max is 100

        // When
        Set<ConstraintViolation<GetLabsRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .hasSize(1)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("Limit cannot exceed 100");
    }

    @Test
    void getLabsRequest_LimitMinimumBoundary_PassesValidation() {
        // Given - Testing minimum boundary (exactly 1)
        GetLabsRequest request = createValidRequest(1, 1);

        // When
        Set<ConstraintViolation<GetLabsRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    void getLabsRequest_LimitMaximumBoundary_PassesValidation() {
        // Given - Testing maximum boundary (exactly 100)
        GetLabsRequest request = createValidRequest(1, 100);

        // When
        Set<ConstraintViolation<GetLabsRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    void getLabsRequest_LimitSignificantlyOverMax_FailsValidation() {
        // Given
        GetLabsRequest request = createValidRequest(1, 1000); // Way over the limit

        // When
        Set<ConstraintViolation<GetLabsRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .hasSize(1)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("Limit cannot exceed 100");
    }

    // Test 4: Multiple validation errors
    @Test
    void getLabsRequest_BothFieldsInvalid_ReturnsAllErrors() {
        // Given - Both fields invalid
        GetLabsRequest request = createValidRequest(0, 101); // Both invalid

        // When
        Set<ConstraintViolation<GetLabsRequest>> violations = validator.validate(request);

        // Then - Should have both validation errors
        assertThat(violations).hasSize(2);
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactlyInAnyOrder(
                        "Page number must be at least 1",
                        "Limit cannot exceed 100"
                );
    }

    @Test
    void getLabsRequest_BothFieldsNegative_ReturnsAllErrors() {
        // Given
        GetLabsRequest request = createValidRequest(-1, -10);

        // When
        Set<ConstraintViolation<GetLabsRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(2);
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactlyInAnyOrder(
                        "Page number must be at least 1",
                        "Limit must be at least 1"
                );
    }

    @Test
    void getLabsRequest_LimitBothMinAndMaxViolation_ReturnsMinError() {
        // Given - Limit violates minimum (negative values can't violate max)
        GetLabsRequest request = createValidRequest(1, -1);

        // When
        Set<ConstraintViolation<GetLabsRequest>> violations = validator.validate(request);

        // Then - Should only get @Min error for negative values
        assertThat(violations)
                .hasSize(1)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("Limit must be at least 1");
    }

    // Test 5: Parameterized tests for invalid page values
    @ParameterizedTest
    @ValueSource(ints = {-100, -10, -1, 0})
    void getLabsRequest_InvalidPageValues_FailsValidation(int invalidPage) {
        // Given
        GetLabsRequest request = createValidRequest(invalidPage, 20);

        // When
        Set<ConstraintViolation<GetLabsRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .hasSize(1)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("Page number must be at least 1");
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 5, 10, 50, 100, 1000, Integer.MAX_VALUE})
    void getLabsRequest_ValidPageValues_PassesValidation(int validPage) {
        // Given
        GetLabsRequest request = createValidRequest(validPage, 20);

        // When
        Set<ConstraintViolation<GetLabsRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = {-50, -1, 0, 101, 150, 1000})
    void getLabsRequest_InvalidLimitValues_FailsValidation(int invalidLimit) {
        // Given
        GetLabsRequest request = createValidRequest(1, invalidLimit);

        // When
        Set<ConstraintViolation<GetLabsRequest>> violations = validator.validate(request);

        // Then - Should have exactly one error (either min or max violation)
        assertThat(violations).hasSize(1);

        // Check that the error message is one of the expected ones
        String errorMessage = violations.iterator().next().getMessage();
        assertThat(errorMessage)
                .isIn("Limit must be at least 1", "Limit cannot exceed 100");
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 10, 20, 50, 99, 100})
    void getLabsRequest_ValidLimitValues_PassesValidation(int validLimit) {
        // Given
        GetLabsRequest request = createValidRequest(1, validLimit);

        // When
        Set<ConstraintViolation<GetLabsRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
    }

    // Test 6: Boundary testing with edge cases
    @Test
    void getLabsRequest_PageBoundaryValues_ValidationResults() {
        // Test multiple boundary scenarios for page
        assertThat(validator.validate(createValidRequest(0, 20))).hasSize(1); // Invalid
        assertThat(validator.validate(createValidRequest(1, 20))).isEmpty();   // Valid
        assertThat(validator.validate(createValidRequest(2, 20))).isEmpty();   // Valid
    }

    @Test
    void getLabsRequest_LimitBoundaryValues_ValidationResults() {
        // Test multiple boundary scenarios for limit
        assertThat(validator.validate(createValidRequest(1, 0))).hasSize(1);   // Invalid (min)
        assertThat(validator.validate(createValidRequest(1, 1))).isEmpty();    // Valid (min boundary)
        assertThat(validator.validate(createValidRequest(1, 100))).isEmpty();  // Valid (max boundary)
        assertThat(validator.validate(createValidRequest(1, 101))).hasSize(1); // Invalid (max)
    }

    // Test 7: Field name verification
    @Test
    void getLabsRequest_PageViolation_ContainsCorrectFieldName() {
        // Given
        GetLabsRequest request = createValidRequest(0, 20);

        // When
        Set<ConstraintViolation<GetLabsRequest>> violations = validator.validate(request);

        // Then - Check the field name that caused the violation
        assertThat(violations)
                .hasSize(1)
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("page");
    }

    @Test
    void getLabsRequest_LimitMinViolation_ContainsCorrectFieldName() {
        // Given
        GetLabsRequest request = createValidRequest(1, 0);

        // When
        Set<ConstraintViolation<GetLabsRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .hasSize(1)
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("limit");
    }

    @Test
    void getLabsRequest_LimitMaxViolation_ContainsCorrectFieldName() {
        // Given
        GetLabsRequest request = createValidRequest(1, 101);

        // When
        Set<ConstraintViolation<GetLabsRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .hasSize(1)
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("limit");
    }

    // Test 8: Data integrity and constructor testing
    @Test
    void getLabsRequest_DefaultConstructor_SetsDefaultValues() {
        // Given & When
        GetLabsRequest request = new GetLabsRequest();

        // Then - Check default values are set correctly
        assertThat(request.getPage()).isEqualTo(1);
        assertThat(request.getLimit()).isEqualTo(20);

        // Verify defaults pass validation
        Set<ConstraintViolation<GetLabsRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    void getLabsRequest_SettersAndGetters_WorkCorrectly() {
        // Given
        GetLabsRequest request = new GetLabsRequest();
        Integer expectedPage = 5;
        Integer expectedLimit = 50;

        // When
        request.setPage(expectedPage);
        request.setLimit(expectedLimit);

        // Then
        assertThat(request.getPage()).isEqualTo(expectedPage);
        assertThat(request.getLimit()).isEqualTo(expectedLimit);

        // Verify it passes validation
        Set<ConstraintViolation<GetLabsRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    // Test 9: Null value handling
    @Test
    void getLabsRequest_NullPage_UsesDefault() {
        // Given
        GetLabsRequest request = new GetLabsRequest();
        request.setPage(null);

        // When
        Set<ConstraintViolation<GetLabsRequest>> violations = validator.validate(request);

        // Then - Null should pass validation if Integer wrapper allows it
        // This depends on how your DTO handles null values
        assertThat(violations).isEmpty();
    }

    @Test
    void getLabsRequest_NullLimit_UsesDefault() {
        // Given
        GetLabsRequest request = new GetLabsRequest();
        request.setLimit(null);

        // When
        Set<ConstraintViolation<GetLabsRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    void getLabsRequest_BothFieldsNull_PassesValidation() {
        // Given
        GetLabsRequest request = new GetLabsRequest();
        request.setPage(null);
        request.setLimit(null);

        // When
        Set<ConstraintViolation<GetLabsRequest>> violations = validator.validate(request);

        // Then - Since using Integer wrapper, nulls might be allowed
        assertThat(violations).isEmpty();
    }

    // Test 10: Real-world scenario testing
    @Test
    void getLabsRequest_CommonPaginationScenarios_PassValidation() {
        // Test common pagination scenarios
        assertThat(validator.validate(createValidRequest(1, 10))).isEmpty();   // First page, small limit
        assertThat(validator.validate(createValidRequest(1, 20))).isEmpty();   // Default values
        assertThat(validator.validate(createValidRequest(5, 50))).isEmpty();   // Mid-range values
        assertThat(validator.validate(createValidRequest(100, 100))).isEmpty(); // Large page, max limit
    }

    @Test
    void getLabsRequest_EdgeCasePaginationScenarios_ProperValidation() {
        // Test edge case scenarios
        assertThat(validator.validate(createValidRequest(1, 1))).isEmpty();     // Minimal valid values
        assertThat(validator.validate(createValidRequest(1000000, 1))).isEmpty(); // Very large page, minimal limit
        assertThat(validator.validate(createValidRequest(1, 100))).isEmpty();   // Minimal page, max limit
    }

    // Helper method to create a valid request with specific values
    private GetLabsRequest createValidRequest(Integer page, Integer limit) {
        GetLabsRequest request = new GetLabsRequest();
        request.setPage(page);
        request.setLimit(limit);
        return request;
    }
}

