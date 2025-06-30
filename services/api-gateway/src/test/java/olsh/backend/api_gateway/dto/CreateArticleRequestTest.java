package olsh.backend.api_gateway.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import olsh.backend.api_gateway.dto.request.CreateArticleRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CreateArticleRequestTest {

    private Validator validator;
    private ValidatorFactory validatorFactory;

    @BeforeEach
    void setUp() {
        // Create Jakarta Bean Validation validator
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
        System.out.println("Setting up DTO validation tests...");
    }

    @AfterEach
    void tearDown() {
        if (validatorFactory != null) {
            validatorFactory.close();
        }
        System.out.println("Cleaning up DTO validation tests...");
    }

    // Test 1: Valid DTO should pass validation
    @Test
    void createArticleRequest_ValidData_PassesValidation() {
        // Given - Create a valid DTO
        CreateArticleRequest validRequest = createValidRequest();

        // When - Validate the DTO
        Set<ConstraintViolation<CreateArticleRequest>> violations = validator.validate(validRequest);

        // Then - Should have no validation errors
        assertThat(violations)
                .isEmpty();
    }

    // Test 2: Title validation tests
    @Test
    void createArticleRequest_NullTitle_FailsValidation() {
        // Given
        CreateArticleRequest request = createValidRequest();
        request.setTitle(null); // Invalid

        // When
        Set<ConstraintViolation<CreateArticleRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .hasSize(1)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("Title is required");
    }

    @Test
    void createArticleRequest_BlankTitle_FailsValidation() {
        // Given
        CreateArticleRequest request = createValidRequest();
        request.setTitle("   "); // Blank (whitespace only)

        // When
        Set<ConstraintViolation<CreateArticleRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .hasSize(2)
                .extracting(ConstraintViolation::getMessage)
                .containsExactlyInAnyOrder("Title is required", "Title must be 10 to 255 characters");
    }

    @Test
    void createArticleRequest_EmptyTitle_FailsValidation() {
        // Given
        CreateArticleRequest request = createValidRequest();
        request.setTitle(""); // Empty string

        // When
        Set<ConstraintViolation<CreateArticleRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .hasSize(2)
                .extracting(ConstraintViolation::getMessage)
                .containsExactlyInAnyOrder("Title is required", "Title must be 10 to 255 characters");
    }

    @Test
    void createArticleRequest_TitleTooLong_FailsValidation() {
        // Given
        CreateArticleRequest request = createValidRequest();
        String longTitle = "A".repeat(256); // 256 characters (limit is 255)
        request.setTitle(longTitle);

        // When
        Set<ConstraintViolation<CreateArticleRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .hasSize(1)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("Title must be 10 to 255 characters");
    }

    @Test
    void createArticleRequest_TitleExactlyMaxLength_PassesValidation() {
        // Given - Testing boundary condition
        CreateArticleRequest request = createValidRequest();
        String maxLengthTitle = "A".repeat(255); // Exactly 255 characters
        request.setTitle(maxLengthTitle);

        // When
        Set<ConstraintViolation<CreateArticleRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
    }

    // Test 3: Short description validation tests
    @Test
    void createArticleRequest_NullShortDesc_FailsValidation() {
        // Given
        CreateArticleRequest request = createValidRequest();
        request.setShort_desc(null);

        // When
        Set<ConstraintViolation<CreateArticleRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .hasSize(1)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("Short description is required");
    }

    @Test
    void createArticleRequest_BlankShortDesc_FailsValidation() {
        // Given
        CreateArticleRequest request = createValidRequest();
        request.setShort_desc("   ");

        // When
        Set<ConstraintViolation<CreateArticleRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .hasSize(2)
                .extracting(ConstraintViolation::getMessage)
                .containsExactlyInAnyOrder("Short description is required", "Short description must be 20 to 1000 characters");
    }

    @Test
    void createArticleRequest_ShortDescTooLong_FailsValidation() {
        // Given
        CreateArticleRequest request = createValidRequest();
        String longDesc = "A".repeat(1001); // 1001 characters (limit is 1000)
        request.setShort_desc(longDesc);

        // When
        Set<ConstraintViolation<CreateArticleRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .hasSize(1)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("Short description must be 20 to 1000 characters");
    }

    @Test
    void createArticleRequest_ShortDescExactlyMaxLength_PassesValidation() {
        // Given
        CreateArticleRequest request = createValidRequest();
        String maxLengthDesc = "A".repeat(1000); // Exactly 1000 characters
        request.setShort_desc(maxLengthDesc);

        // When
        Set<ConstraintViolation<CreateArticleRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
    }

    // Test 4: PDF file validation tests
    @Test
    void createArticleRequest_NullPdfFile_FailsValidation() {
        // Given
        CreateArticleRequest request = createValidRequest();
        request.setPdf_file(null);

        // When
        Set<ConstraintViolation<CreateArticleRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .hasSize(1)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("PDF file is required");
    }

    // Test 5: Multiple validation errors
    @Test
    void createArticleRequest_MultipleInvalidFields_ReturnsAllErrors() {
        // Given - Multiple invalid fields
        CreateArticleRequest request = new CreateArticleRequest();
        request.setTitle(null); // Invalid
        request.setShort_desc(""); // Invalid
        request.setPdf_file(null); // Invalid

        // When
        Set<ConstraintViolation<CreateArticleRequest>> violations = validator.validate(request);

        // Then - Should have all 3 validation errors
        assertThat(violations).hasSize(4);
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactlyInAnyOrder(
                        "Title is required",
                        "Short description is required",
                        "Short description must be 20 to 1000 characters",
                        "PDF file is required"
                );
    }

    // Test 6: Parameterized test for different invalid titles
    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t", "\n"})
    void createArticleRequest_BlankTitleVariations_FailsValidation(String invalidTitle) {
        // Given
        CreateArticleRequest request = createValidRequest();
        request.setTitle(invalidTitle);

        // When
        Set<ConstraintViolation<CreateArticleRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .hasSize(2)
                .extracting(ConstraintViolation::getMessage)
                .containsExactlyInAnyOrder("Title is required", "Title must be 10 to 255 characters");
    }

    // Test 7: Field-specific constraint violation testing
    @Test
    void createArticleRequest_TitleTooLong_ContainsCorrectFieldName() {
        // Given
        CreateArticleRequest request = createValidRequest();
        request.setTitle("A".repeat(256));

        // When
        Set<ConstraintViolation<CreateArticleRequest>> violations = validator.validate(request);

        // Then - Check the field name that caused the violation
        assertThat(violations)
                .hasSize(1)
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("title");
    }

    @Test
    void createArticleRequest_ShortDescTooLong_ContainsCorrectFieldName() {
        // Given
        CreateArticleRequest request = createValidRequest();
        request.setShort_desc("A".repeat(1001));

        // When
        Set<ConstraintViolation<CreateArticleRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .hasSize(1)
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("short_desc");
    }

    // Helper method to create a valid request
    private CreateArticleRequest createValidRequest() {
        MultipartFile validPdfFile = new MockMultipartFile(
                "file",
                "document.pdf",
                "application/pdf",
                "PDF content".getBytes()
        );

        return new CreateArticleRequest(
                "Valid Article Title",
                "This is a valid short description for the article",
                validPdfFile
        );
    }
}

