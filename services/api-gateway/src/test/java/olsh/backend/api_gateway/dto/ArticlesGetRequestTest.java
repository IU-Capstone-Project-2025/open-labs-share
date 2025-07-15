package olsh.backend.api_gateway.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import olsh.backend.api_gateway.dto.request.ArticlesGetRequest;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ArticlesGetRequestTest {

    private Validator validator;
    private ValidatorFactory validatorFactory;

    @BeforeEach
    void setUp() {
        // Create Jakarta Bean Validation validator
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
        System.out.println("Setting up GetArticlesRequest validation tests...");
    }

    @AfterEach
    void tearDown() {
        if (validatorFactory != null) {
            validatorFactory.close();
        }
        System.out.println("Cleaning up GetArticlesRequest validation tests...");
    }

    // Test 1: Valid DTO should pass validation
    @Test
    void getArticlesRequest_ValidData_PassesValidation() {
        // Given - Create a valid DTO
        ArticlesGetRequest validRequest = createValidRequest();

        // When - Validate the DTO
        Set<ConstraintViolation<ArticlesGetRequest>> violations = validator.validate(validRequest);

        // Then - Should have no validation errors
        assertThat(violations).isEmpty();
    }

    @Test
    void getArticlesRequest_DefaultValues_PassesValidation() {
        // Given - Using default constructor (should use default values)
        ArticlesGetRequest requestWithDefaults = new ArticlesGetRequest();

        // When
        Set<ConstraintViolation<ArticlesGetRequest>> violations = validator.validate(requestWithDefaults);

        // Then - Default values should be valid
        assertThat(violations).isEmpty();
        assertThat(requestWithDefaults.getPage()).isEqualTo(1);
        assertThat(requestWithDefaults.getLimit()).isEqualTo(20);
    }

    // Test 2: Page validation tests
    @Test
    void getArticlesRequest_ZeroPage_FailsValidation() {
        // Given
        ArticlesGetRequest request = createValidRequest();
        request.setPage(0); // Invalid - should be >= 1

        // When
        Set<ConstraintViolation<ArticlesGetRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .hasSize(1)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("Page must be greater than 0");

        // Check the field name
        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("page");
    }

    @Test
    void getArticlesRequest_NegativePage_FailsValidation() {
        // Given
        ArticlesGetRequest request = createValidRequest();
        request.setPage(-1); // Invalid

        // When
        Set<ConstraintViolation<ArticlesGetRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .hasSize(1)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("Page must be greater than 0");
    }

    @Test
    void getArticlesRequest_PageOne_PassesValidation() {
        // Given - Testing boundary condition (minimum valid value)
        ArticlesGetRequest request = createValidRequest();
        request.setPage(1); // Exactly the minimum allowed

        // When
        Set<ConstraintViolation<ArticlesGetRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    void getArticlesRequest_LargePage_PassesValidation() {
        // Given - Testing that there's no upper limit on page
        ArticlesGetRequest request = createValidRequest();
        request.setPage(Integer.MAX_VALUE); // Very large page number

        // When
        Set<ConstraintViolation<ArticlesGetRequest>> violations = validator.validate(request);

        // Then - Should pass (no max constraint on page)
        assertThat(violations).isEmpty();
    }

    // Test 3: Limit validation tests
    @Test
    void getArticlesRequest_ZeroLimit_FailsValidation() {
        // Given
        ArticlesGetRequest request = createValidRequest();
        request.setLimit(0); // Invalid - should be >= 1

        // When
        Set<ConstraintViolation<ArticlesGetRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .hasSize(1)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("Limit must be greater than 0");
    }

    @Test
    void getArticlesRequest_NegativeLimit_FailsValidation() {
        // Given
        ArticlesGetRequest request = createValidRequest();
        request.setLimit(-5); // Invalid

        // When
        Set<ConstraintViolation<ArticlesGetRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .hasSize(1)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("Limit must be greater than 0");
    }

    @Test
    void getArticlesRequest_LimitExceedsMaximum_FailsValidation() {
        // Given
        ArticlesGetRequest request = createValidRequest();
        request.setLimit(101); // Invalid - max is 100

        // When
        Set<ConstraintViolation<ArticlesGetRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .hasSize(1)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("Limit must not exceed 100");
    }

    @Test
    void getArticlesRequest_LimitMinimumBoundary_PassesValidation() {
        // Given - Testing minimum boundary (exactly 1)
        ArticlesGetRequest request = createValidRequest();
        request.setLimit(1); // Exactly the minimum allowed

        // When
        Set<ConstraintViolation<ArticlesGetRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    void getArticlesRequest_LimitMaximumBoundary_PassesValidation() {
        // Given - Testing maximum boundary (exactly 100)
        ArticlesGetRequest request = createValidRequest();
        request.setLimit(100); // Exactly the maximum allowed

        // When
        Set<ConstraintViolation<ArticlesGetRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
    }

    // Test 4: Multiple validation errors
    @Test
    void getArticlesRequest_BothFieldsInvalid_ReturnsAllErrors() {
        // Given - Both fields invalid
        ArticlesGetRequest request = new ArticlesGetRequest();
        request.setPage(0); // Invalid
        request.setLimit(101); // Invalid

        // When
        Set<ConstraintViolation<ArticlesGetRequest>> violations = validator.validate(request);

        // Then - Should have both validation errors
        assertThat(violations).hasSize(2);
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactlyInAnyOrder(
                        "Page must be greater than 0",
                        "Limit must not exceed 100"
                );
    }

    @Test
    void getArticlesRequest_MultipleViolationsOnLimit_ReturnsAllErrors() {
        // Given - This tests what happens with overlapping constraints
        ArticlesGetRequest request = createValidRequest();
        request.setLimit(-1); // This violates @Min constraint

        // When
        Set<ConstraintViolation<ArticlesGetRequest>> violations = validator.validate(request);

        // Then - Should only have one error (the @Min constraint)
        assertThat(violations)
                .hasSize(1)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("Limit must be greater than 0");
    }

    // Test 5: Parameterized tests for invalid page values
    @ParameterizedTest
    @ValueSource(ints = {-100, -1, 0})
    void getArticlesRequest_InvalidPageValues_FailsValidation(int invalidPage) {
        // Given
        ArticlesGetRequest request = createValidRequest();
        request.setPage(invalidPage);

        // When
        Set<ConstraintViolation<ArticlesGetRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .hasSize(1)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("Page must be greater than 0");
    }

    @ParameterizedTest
    @ValueSource(ints = {-50, -1, 0, 101, 150, 1000})
    void getArticlesRequest_InvalidLimitValues_FailsValidation(int invalidLimit) {
        // Given
        ArticlesGetRequest request = createValidRequest();
        request.setLimit(invalidLimit);

        // When
        Set<ConstraintViolation<ArticlesGetRequest>> violations = validator.validate(request);

        // Then - Should have exactly one error (either min or max violation)
        assertThat(violations).hasSize(1);

        // Check that the error message is one of the expected ones
        String errorMessage = violations.iterator().next().getMessage();
        assertThat(errorMessage)
                .isIn("Limit must be greater than 0", "Limit must not exceed 100");
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 10, 20, 50, 99, 100})
    void getArticlesRequest_ValidLimitValues_PassesValidation(int validLimit) {
        // Given
        ArticlesGetRequest request = createValidRequest();
        request.setLimit(validLimit);

        // When
        Set<ConstraintViolation<ArticlesGetRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
    }

    // Test 6: Constructor testing
    @Test
    void getArticlesRequest_AllArgsConstructor_CreatesCorrectly() {
        // Given
        Integer expectedPage = 5;
        Integer expectedLimit = 50;

        // When
        ArticlesGetRequest request = new ArticlesGetRequest(expectedPage, expectedLimit, "", "");

        // Then
        assertThat(request.getPage()).isEqualTo(expectedPage);
        assertThat(request.getLimit()).isEqualTo(expectedLimit);

        // Verify it passes validation
        Set<ConstraintViolation<ArticlesGetRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    void getArticlesRequest_NoArgsConstructor_UsesDefaults() {
        // Given & When
        ArticlesGetRequest request = new ArticlesGetRequest();

        // Then - Check default values are set
        assertThat(request.getPage()).isEqualTo(1);
        assertThat(request.getLimit()).isEqualTo(20);
    }

    // Test 7: Null value handling
    @Test
    void getArticlesRequest_NullPage_UsesDefault() {
        // Given
        ArticlesGetRequest request = new ArticlesGetRequest();
        request.setPage(null);

        // When
        Set<ConstraintViolation<ArticlesGetRequest>> violations = validator.validate(request);

        // Then - Null should pass validation (primitives have default handling)
        assertThat(violations).isEmpty();
    }

    @Test
    void getArticlesRequest_NullLimit_UsesDefault() {
        // Given
        ArticlesGetRequest request = new ArticlesGetRequest();
        request.setLimit(null);

        // When
        Set<ConstraintViolation<ArticlesGetRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
    }

    // Helper method to create a valid request
    private ArticlesGetRequest createValidRequest() {
        return new ArticlesGetRequest(1, 20, "", "");
    }
}
