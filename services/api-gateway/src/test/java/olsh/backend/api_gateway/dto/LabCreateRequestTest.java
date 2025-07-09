package olsh.backend.api_gateway.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import olsh.backend.api_gateway.dto.request.LabCreateRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class LabCreateRequestTest {

    private Validator validator;
    private ValidatorFactory validatorFactory;

    @BeforeEach
    void setUp() {
        // Create Jakarta Bean Validation validator
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
        System.out.println("Setting up CreateLabRequest validation tests...");
    }

    @AfterEach
    void tearDown() {
        if (validatorFactory != null) {
            validatorFactory.close();
        }
        System.out.println("Cleaning up CreateLabRequest validation tests...");
    }

    // Test 1: Valid DTO should pass validation
    @Test
    void createLabRequest_ValidData_PassesValidation() {
        // Given - Create a valid DTO
        LabCreateRequest validRequest = createValidRequest();

        // When - Validate the DTO
        Set<ConstraintViolation<LabCreateRequest>> violations = validator.validate(validRequest);

        // Then - Should have no validation errors
        assertThat(violations).isEmpty();
    }

    @Test
    void createLabRequest_ValidDataWithAssets_PassesValidation() {
        // Given - Valid request with assets
        LabCreateRequest validRequest = createValidRequestWithAssets();

        // When
        Set<ConstraintViolation<LabCreateRequest>> violations = validator.validate(validRequest);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    void createLabRequest_ValidDataWithoutAssets_PassesValidation() {
        // Given - Valid request without assets (null assets)
        LabCreateRequest validRequest = createValidRequest();
        validRequest.setAssets(null); // Assets are optional

        // When
        Set<ConstraintViolation<LabCreateRequest>> violations = validator.validate(validRequest);

        // Then
        assertThat(violations).isEmpty();
    }

    // Test 2: Title validation tests
    @Test
    void createLabRequest_NullTitle_FailsValidation() {
        // Given
        LabCreateRequest request = createValidRequest();
        request.setTitle(null); // Invalid

        // When
        Set<ConstraintViolation<LabCreateRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .hasSize(1)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("Title is required");
    }

    @Test
    void createLabRequest_BlankTitle_FailsValidation() {
        // Given
        LabCreateRequest request = createValidRequest();
        request.setTitle("   "); // Blank (whitespace only)

        // When
        Set<ConstraintViolation<LabCreateRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .hasSize(2)
                .extracting(ConstraintViolation::getMessage)
                .containsExactlyInAnyOrder("Title must be 10 to 255 characters", "Title is required");
    }

    @Test
    void createLabRequest_EmptyTitle_FailsValidation() {
        // Given
        LabCreateRequest request = createValidRequest();
        request.setTitle(""); // Empty string

        // When
        Set<ConstraintViolation<LabCreateRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .hasSize(2)
                .extracting(ConstraintViolation::getMessage)
                .containsExactlyInAnyOrder("Title must be 10 to 255 characters", "Title is required");
    }

    @Test
    void createLabRequest_ValidTitle_PassesValidation() {
        // Given
        LabCreateRequest request = createValidRequest();
        request.setTitle("Introduction to Data Structures"); // Valid title

        // When
        Set<ConstraintViolation<LabCreateRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
    }

    // Test 3: Short description validation tests
    @Test
    void createLabRequest_NullShortDesc_FailsValidation() {
        // Given
        LabCreateRequest request = createValidRequest();
        request.setShort_desc(null);

        // When
        Set<ConstraintViolation<LabCreateRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .hasSize(1)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("Short description is required");
    }

    @Test
    void createLabRequest_BlankShortDesc_FailsValidation() {
        // Given
        LabCreateRequest request = createValidRequest();
        request.setShort_desc("   "); // Blank

        // When
        Set<ConstraintViolation<LabCreateRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .hasSize(2)
                .extracting(ConstraintViolation::getMessage)
                .containsExactlyInAnyOrder("Short description is required", "Short description must be 20 to 1000 symbols");
    }

    @Test
    void createLabRequest_EmptyShortDesc_FailsValidation() {
        // Given
        LabCreateRequest request = createValidRequest();
        request.setShort_desc(""); // Empty

        // When
        Set<ConstraintViolation<LabCreateRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .hasSize(2)
                .extracting(ConstraintViolation::getMessage)
                .containsExactlyInAnyOrder("Short description is required", "Short description must be 20 to 1000 symbols");
    }

    @Test
    void createLabRequest_ValidShortDesc_PassesValidation() {
        // Given
        LabCreateRequest request = createValidRequest();
        request.setShort_desc("Learn about basic data structures and their implementations");

        // When
        Set<ConstraintViolation<LabCreateRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    void createLabRequest_TitleTooLong_FailsValidation() {
        // Given
        LabCreateRequest request = createValidRequest();
        String longTitle = "A".repeat(256); // 256 characters (limit is 255)
        request.setTitle(longTitle);

        // When
        Set<ConstraintViolation<LabCreateRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .hasSize(1)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("Title must be 10 to 255 characters");
    }

    @Test
    void createLabRequest_TitleExactlyMaxLength_PassesValidation() {
        // Given - Testing boundary condition
        LabCreateRequest request = createValidRequest();
        String maxLengthTitle = "A".repeat(255); // Exactly 255 characters
        request.setTitle(maxLengthTitle);

        // When
        Set<ConstraintViolation<LabCreateRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    void createLabRequest_TitleSignificantlyTooLong_FailsValidation() {
        // Given
        LabCreateRequest request = createValidRequest();
        String veryLongTitle = "This is a very long title that definitely exceeds the maximum allowed length for a lab title. ".repeat(10); // Much longer than 255
        request.setTitle(veryLongTitle);

        // When
        Set<ConstraintViolation<LabCreateRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .hasSize(1)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("Title must be 10 to 255 characters");

        // Verify the actual length is way over the limit
        assertThat(veryLongTitle.length()).isGreaterThan(255);
    }

    @Test
    void createLabRequest_TitleOneCharacterOverLimit_FailsValidation() {
        // Given - Testing edge case (exactly 1 char over limit)
        LabCreateRequest request = createValidRequest();
        String titleOverLimit = "A".repeat(256); // Exactly 256 characters (1 over limit)
        request.setTitle(titleOverLimit);

        // When
        Set<ConstraintViolation<LabCreateRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .hasSize(1)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("Title must be 10 to 255 characters");

        // Verify exact length
        assertThat(titleOverLimit.length()).isEqualTo(256);
    }

    @Test
    void createLabRequest_ShortDescTooLong_FailsValidation() {
        // Given
        LabCreateRequest request = createValidRequest();
        String longDesc = "A".repeat(1001); // 1001 characters (limit is 1000)
        request.setShort_desc(longDesc);

        // When
        Set<ConstraintViolation<LabCreateRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .hasSize(1)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("Short description must be 20 to 1000 symbols");
    }

    @Test
    void createLabRequest_ShortDescExactlyMaxLength_PassesValidation() {
        // Given - Testing boundary condition
        LabCreateRequest request = createValidRequest();
        String maxLengthDesc = "A".repeat(1000); // Exactly 1000 characters
        request.setShort_desc(maxLengthDesc);

        // When
        Set<ConstraintViolation<LabCreateRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    void createLabRequest_ShortDescSignificantlyTooLong_FailsValidation() {
        // Given
        LabCreateRequest request = createValidRequest();
        String veryLongDesc = "This is a very long description that definitely exceeds the maximum allowed length for a lab short description. It contains multiple sentences and goes on and on about the lab content, requirements, learning objectives, and other details that should probably be in the full lab instructions rather than the short description. ".repeat(5);
        request.setShort_desc(veryLongDesc);

        // When
        Set<ConstraintViolation<LabCreateRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .hasSize(1)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("Short description must be 20 to 1000 symbols");

        // Verify the actual length is way over the limit
        assertThat(veryLongDesc.length()).isGreaterThan(1000);
    }

    @Test
    void createLabRequest_ShortDescOneCharacterOverLimit_FailsValidation() {
        // Given - Testing edge case (exactly 1 char over limit)
        LabCreateRequest request = createValidRequest();
        String descOverLimit = "A".repeat(1001); // Exactly 1001 characters (1 over limit)
        request.setShort_desc(descOverLimit);

        // When
        Set<ConstraintViolation<LabCreateRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .hasSize(1)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("Short description must be 20 to 1000 symbols");

        // Verify exact length
        assertThat(descOverLimit.length()).isEqualTo(1001);
    }


    // Test 4: Markdown file validation tests
    @Test
    void createLabRequest_NullMdFile_FailsValidation() {
        // Given
        LabCreateRequest request = createValidRequest();
        request.setMd_file(null);

        // When
        Set<ConstraintViolation<LabCreateRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .hasSize(1)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("Markdown file is required");
    }

    @Test
    void createLabRequest_ValidMdFile_PassesValidation() {
        // Given
        LabCreateRequest request = createValidRequest();
        MockMultipartFile validMdFile = new MockMultipartFile(
                "md_file",
                "lab-instructions.md",
                "text/markdown",
                "# Lab Instructions\nThis is a markdown file".getBytes()
        );
        request.setMd_file(validMdFile);

        // When
        Set<ConstraintViolation<LabCreateRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    void createLabRequest_EmptyMdFile_PassesValidation() {
        // Given - Empty file should pass @NotNull but might fail business logic later
        LabCreateRequest request = createValidRequest();
        MockMultipartFile emptyMdFile = new MockMultipartFile(
                "md_file",
                "lab-instructions.md",
                "text/markdown",
                new byte[0] // Empty content
        );
        request.setMd_file(emptyMdFile);

        // When
        Set<ConstraintViolation<LabCreateRequest>> violations = validator.validate(request);

        // Then - @NotNull only checks for null, not empty content
        assertThat(violations).isEmpty();
    }

    // Test 5: Assets validation tests (assets are optional)
    @Test
    void createLabRequest_NullAssets_PassesValidation() {
        // Given
        LabCreateRequest request = createValidRequest();
        request.setAssets(null); // Assets are optional

        // When
        Set<ConstraintViolation<LabCreateRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    void createLabRequest_EmptyAssetsArray_PassesValidation() {
        // Given
        LabCreateRequest request = createValidRequest();
        request.setAssets(new MultipartFile[0]); // Empty array

        // When
        Set<ConstraintViolation<LabCreateRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    void createLabRequest_ValidAssets_PassesValidation() {
        // Given
        LabCreateRequest request = createValidRequestWithAssets();

        // When
        Set<ConstraintViolation<LabCreateRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    void createLabRequest_AssetsWithNullElements_PassesValidation() {
        // Given - Array with null elements (validation doesn't check array contents)
        LabCreateRequest request = createValidRequest();
        MultipartFile[] assetsWithNull = {
                new MockMultipartFile("asset1", "image1.png", "image/png", "PNG content".getBytes()),
                null, // Null element in array
                new MockMultipartFile("asset3", "image3.jpg", "image/jpeg", "JPEG content".getBytes())
        };
        request.setAssets(assetsWithNull);

        // When
        Set<ConstraintViolation<LabCreateRequest>> violations = validator.validate(request);

        // Then - Jakarta validation doesn't validate array contents by default
        assertThat(violations).isEmpty();
    }

    // Test 6: Multiple validation errors
    @Test
    void createLabRequest_AllFieldsInvalid_ReturnsAllErrors() {
        // Given - All required fields invalid
        LabCreateRequest request = new LabCreateRequest();
        request.setTitle(null); // Invalid
        request.setShort_desc(""); // Invalid
        request.setMd_file(null); // Invalid
        request.setAssets(null); // Valid (optional)

        // When
        Set<ConstraintViolation<LabCreateRequest>> violations = validator.validate(request);

        // Then - Should have 3 validation errors
        assertThat(violations).hasSize(4);
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactlyInAnyOrder(
                        "Title is required",
                        "Short description is required",
                        "Short description must be 20 to 1000 symbols",
                        "Markdown file is required"
                );
    }

    @Test
    void createLabRequest_SomeFieldsInvalid_ReturnsPartialErrors() {
        // Given - Only some fields invalid
        LabCreateRequest request = createValidRequest();
        request.setTitle("   "); // Invalid
        request.setShort_desc(null); // Invalid
        // md_file and assets remain valid

        // When
        Set<ConstraintViolation<LabCreateRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(3);
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactlyInAnyOrder(
                        "Title is required",
                        "Short description is required",
                        "Title must be 10 to 255 characters"
                );
    }

    // Test 7: Field-specific constraint violation testing
    @Test
    void createLabRequest_TitleViolation_ContainsCorrectFieldName() {
        // Given
        LabCreateRequest request = createValidRequest();
        request.setTitle(null);

        // When
        Set<ConstraintViolation<LabCreateRequest>> violations = validator.validate(request);

        // Then - Check the field name that caused the violation
        assertThat(violations)
                .hasSize(1)
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("title");
    }

    @Test
    void createLabRequest_MdFileViolation_ContainsCorrectFieldName() {
        // Given
        LabCreateRequest request = createValidRequest();
        request.setMd_file(null);

        // When
        Set<ConstraintViolation<LabCreateRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .hasSize(1)
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("md_file");
    }

    // Test 8: Data integrity and getter/setter testing
    @Test
    void createLabRequest_SettersAndGetters_WorkCorrectly() {
        // Given
        LabCreateRequest request = new LabCreateRequest();
        String expectedTitle = "Data Structures Lab";
        String expectedShortDesc = "Learn about arrays and linked lists";
        MockMultipartFile expectedMdFile = new MockMultipartFile(
                "md_file", "lab.md", "text/markdown", "# Lab content".getBytes()
        );
        MultipartFile[] expectedAssets = {
                new MockMultipartFile("asset1", "image1.png", "image/png", "PNG".getBytes())
        };

        // When
        request.setTitle(expectedTitle);
        request.setShort_desc(expectedShortDesc);
        request.setMd_file(expectedMdFile);
        request.setAssets(expectedAssets);

        // Then
        assertThat(request.getTitle()).isEqualTo(expectedTitle);
        assertThat(request.getShort_desc()).isEqualTo(expectedShortDesc);
        assertThat(request.getMd_file()).isEqualTo(expectedMdFile);
        assertThat(request.getAssets()).isEqualTo(expectedAssets);

        // Verify the object is valid
        Set<ConstraintViolation<LabCreateRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    // Helper methods to create valid requests
    private LabCreateRequest createValidRequest() {
        LabCreateRequest request = new LabCreateRequest();
        request.setTitle("Introduction to Data Structures");
        request.setShort_desc("Learn about basic data structures and their implementations");

        MockMultipartFile validMdFile = new MockMultipartFile(
                "md_file",
                "lab-instructions.md",
                "text/markdown",
                "# Lab Instructions\nThis is a comprehensive lab about data structures.".getBytes()
        );
        request.setMd_file(validMdFile);

        // No assets by default
        request.setAssets(null);

        return request;
    }

    private LabCreateRequest createValidRequestWithAssets() {
        LabCreateRequest request = createValidRequest();

        MultipartFile[] assets = {
                new MockMultipartFile(
                        "asset1",
                        "diagram.png",
                        "image/png",
                        "PNG diagram content".getBytes()
                ),
                new MockMultipartFile(
                        "asset2",
                        "example.jpg",
                        "image/jpeg",
                        "JPEG example content".getBytes()
                ),
                new MockMultipartFile(
                        "asset3",
                        "dataset.csv",
                        "text/csv",
                        "CSV,data,content".getBytes()
                )
        };
        request.setAssets(assets);

        return request;
    }
}

