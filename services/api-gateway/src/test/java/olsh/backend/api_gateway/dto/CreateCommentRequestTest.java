package olsh.backend.api_gateway.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import olsh.backend.api_gateway.dto.request.CreateCommentRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CreateCommentRequest - DTO Validation Tests")
class CreateCommentRequestTest {

    private Validator validator;
    private ValidatorFactory validatorFactory;

    @BeforeEach
    @DisplayName("Set up Jakarta Bean Validation validator")
    void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
        System.out.println("Setting up CreateCommentRequest validation tests...");
    }

    @AfterEach
    @DisplayName("Clean up validation factory")
    void tearDown() {
        if (validatorFactory != null) {
            validatorFactory.close();
        }
        System.out.println("Cleaning up CreateCommentRequest validation tests...");
    }

    // Test 1: Valid requests should pass validation
    @Test
    @DisplayName("Should pass validation with valid content and parent ID")
    void createCommentRequest_ValidContentWithParentId_PassesValidation() {
        // Given
        CreateCommentRequest request = new CreateCommentRequest(
                "This is a valid comment content",
                "parent-comment-123"
        );

        // When
        Set<ConstraintViolation<CreateCommentRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should pass validation with valid content and null parent ID")
    void createCommentRequest_ValidContentWithNullParentId_PassesValidation() {
        // Given
        CreateCommentRequest request = new CreateCommentRequest(
                "This is a valid top-level comment",
                null // No parent - top-level comment
        );

        // When
        Set<ConstraintViolation<CreateCommentRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should pass validation with minimal valid content")
    void createCommentRequest_MinimalValidContent_PassesValidation() {
        // Given
        CreateCommentRequest request = new CreateCommentRequest("a", null);

        // When
        Set<ConstraintViolation<CreateCommentRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
    }

    // Test 2: Invalid content should fail validation
    @ParameterizedTest(name = "Should fail validation when content is: [{0}]")
    @NullAndEmptySource
    @DisplayName("Content validation - Null and Empty Content")
    void createCommentRequest_NullAndEmptyContent_FailsValidation(String invalidContent) {
        // Given
        CreateCommentRequest request = new CreateCommentRequest(invalidContent, null);

        // When
        Set<ConstraintViolation<CreateCommentRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .hasSize(1)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("Content cannot be blank");
    }

    @ParameterizedTest(name = "Should fail validation for blank content: [{0}]")
    @MethodSource("provideBlankContentVariations")
    @DisplayName("Content validation - Blank Content Variations")
    void createCommentRequest_BlankContent_FailsValidation(String blankContent) {
        // Given
        CreateCommentRequest request = new CreateCommentRequest(blankContent, null);

        // When
        Set<ConstraintViolation<CreateCommentRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .hasSize(1)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("Content cannot be blank");
    }

    // Data provider for blank content variations
    static Stream<String> provideBlankContentVariations() {
        String[] blankContents = {
                " ",                    // Single space
                "   ",                  // Multiple spaces
                "\t",                   // Tab
                "\n",                   // Newline
                "\r",                   // Carriage return
                "\f",                   // Form feed
                "\t\t",                 // Multiple tabs
                "\n\n",                 // Multiple newlines
                " \t ",                 // Mixed spaces and tabs
                " \n\r ",               // Complex whitespace mix
                "  \t\n\r  ",          // Multiple different whitespace
        };
        return Stream.of(blankContents);
    }

    // Test 3: Field name verification
    @Test
    @DisplayName("Should identify 'content' field in constraint violation")
    void createCommentRequest_InvalidContent_ContainsCorrectFieldName() {
        // Given
        CreateCommentRequest request = new CreateCommentRequest(null, "parent-123");

        // When
        Set<ConstraintViolation<CreateCommentRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .hasSize(1)
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("content");
    }

    // Test 4: Parent ID field testing (no validation constraints)
    @Test
    @DisplayName("Should accept any parent ID format (no validation on parentId)")
    void createCommentRequest_VariousParentIdFormats_PassesValidation() {
        // Given - Test various parent ID formats
        String[] parentIds = {
                null,                           // No parent
                "",                            // Empty string
                "   ",                         // Whitespace
                "simple-id",                   // Simple format
                "uuid-12345678-1234-1234",     // UUID-like
                "123",                         // Numeric
                "very-long-parent-comment-id-with-many-characters-123456789"
        };

        String validContent = "Valid comment content";

        // When & Then
        for (String parentId : parentIds) {
            CreateCommentRequest request = new CreateCommentRequest(validContent, parentId);
            Set<ConstraintViolation<CreateCommentRequest>> violations = validator.validate(request);

            assertThat(violations)
                    .as("Parent ID '%s' should be accepted", parentId)
                    .isEmpty();
        }
    }

    // Test 5: Constructor and getter/setter testing
    @Test
    @DisplayName("Should correctly set and get values through constructors and accessors")
    void createCommentRequest_ConstructorsAndAccessors_WorkCorrectly() {
        // Given
        String expectedContent = "Test comment content";
        String expectedParentId = "parent-comment-456";

        // When - Test all-args constructor
        CreateCommentRequest request1 = new CreateCommentRequest(expectedContent, expectedParentId);

        // Then
        assertThat(request1.getContent()).isEqualTo(expectedContent);
        assertThat(request1.getParentId()).isEqualTo(expectedParentId);

        // When - Test no-args constructor + setters
        CreateCommentRequest request2 = new CreateCommentRequest();
        request2.setContent(expectedContent);
        request2.setParentId(expectedParentId);

        // Then
        assertThat(request2.getContent()).isEqualTo(expectedContent);
        assertThat(request2.getParentId()).isEqualTo(expectedParentId);

        // Verify both requests pass validation
        assertThat(validator.validate(request1)).isEmpty();
        assertThat(validator.validate(request2)).isEmpty();
    }

    // Test 6: Edge cases with special characters
    @Test
    @DisplayName("Should accept content with special characters and Unicode")
    void createCommentRequest_SpecialCharactersInContent_PassesValidation() {
        // Given
        String[] specialContents = {
                "Comment with émojis 🚀 and ñice çharacters",
                "Comment with symbols: @#$%^&*()_+{}|:<>?",
                "Comment with\nnewlines\tand\ttabs",
                "Comment with \"quotes\" and 'apostrophes'",
                "Comment with [brackets] and (parentheses)",
                "HTML-like content: <div>Hello</div>",
                "JSON-like: {\"message\": \"test\"}",
                "Very long comment that goes on and on and contains lots of text to test that there are no length restrictions on the content field which should be fine since there's no @Size annotation present"
        };

        // When & Then
        for (String content : specialContents) {
            CreateCommentRequest request = new CreateCommentRequest(content, null);
            Set<ConstraintViolation<CreateCommentRequest>> violations = validator.validate(request);

            assertThat(violations)
                    .as("Content '%s' should be valid", content)
                    .isEmpty();
        }
    }

    // Test 7: Boundary testing for whitespace trimming
    @Test
    @DisplayName("Should fail validation for content that becomes empty after trimming")
    void createCommentRequest_ContentEmptyAfterTrimming_FailsValidation() {
        // Given - Content that only contains whitespace
        CreateCommentRequest request = new CreateCommentRequest("   \t\n   ", "parent-123");

        // When
        Set<ConstraintViolation<CreateCommentRequest>> violations = validator.validate(request);

        // Then - @NotBlank should catch this
        assertThat(violations)
                .hasSize(1)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("Content cannot be blank");
    }

    // Test 8: Multiple instantiation patterns
    @Test
    @DisplayName("Should handle various instantiation patterns correctly")
    void createCommentRequest_VariousInstantiationPatterns_WorkCorrectly() {
        // Pattern 1: All-args constructor
        CreateCommentRequest request1 = new CreateCommentRequest("Content 1", "parent-1");

        // Pattern 2: No-args constructor with setters
        CreateCommentRequest request2 = new CreateCommentRequest();
        request2.setContent("Content 2");
        request2.setParentId("parent-2");

        // Pattern 3: Partial setting (only content)
        CreateCommentRequest request3 = new CreateCommentRequest("Content 3", null);

        // Verify all patterns work and pass validation
        assertThat(validator.validate(request1)).isEmpty();
        assertThat(validator.validate(request2)).isEmpty();
        assertThat(validator.validate(request3)).isEmpty();

        // Verify values are set correctly
        assertThat(request1.getContent()).isEqualTo("Content 1");
        assertThat(request1.getParentId()).isEqualTo("parent-1");

        assertThat(request2.getContent()).isEqualTo("Content 2");
        assertThat(request2.getParentId()).isEqualTo("parent-2");

        assertThat(request3.getContent()).isEqualTo("Content 3");
        assertThat(request3.getParentId()).isNull();
    }
}

