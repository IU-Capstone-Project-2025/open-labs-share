package olsh.backend.api_gateway.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import olsh.backend.api_gateway.dto.request.UpdateCommentRequest;
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

@DisplayName("UpdateCommentRequest - DTO Validation Tests")
class UpdateCommentRequestTest {

    private Validator validator;
    private ValidatorFactory validatorFactory;

    @BeforeEach
    @DisplayName("Set up Jakarta Bean Validation validator")
    void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
        System.out.println("Setting up UpdateCommentRequest validation tests...");
    }

    @AfterEach
    @DisplayName("Clean up validation factory")
    void tearDown() {
        if (validatorFactory != null) {
            validatorFactory.close();
        }
        System.out.println("Cleaning up UpdateCommentRequest validation tests...");
    }

    // Test 1: Valid content should pass validation
    @Test
    @DisplayName("Should pass validation with valid content")
    void updateCommentRequest_ValidContent_PassesValidation() {
        // Given
        UpdateCommentRequest request = new UpdateCommentRequest("Updated comment content");

        // When
        Set<ConstraintViolation<UpdateCommentRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should pass validation with minimal valid content")
    void updateCommentRequest_MinimalValidContent_PassesValidation() {
        // Given
        UpdateCommentRequest request = new UpdateCommentRequest("a");

        // When
        Set<ConstraintViolation<UpdateCommentRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should pass validation with long content")
    void updateCommentRequest_LongContent_PassesValidation() {
        // Given
        String longContent = "This is a very long updated comment that contains lots of text to verify that there are no length restrictions on the content field since there's no @Size annotation present in the UpdateCommentRequest class.";
        UpdateCommentRequest request = new UpdateCommentRequest(longContent);

        // When
        Set<ConstraintViolation<UpdateCommentRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
    }

    // Test 2: Invalid content should fail validation
    @ParameterizedTest(name = "Should fail validation when content is: [{0}]")
    @NullAndEmptySource
    @DisplayName("Content validation - Null and Empty Content")
    void updateCommentRequest_NullAndEmptyContent_FailsValidation(String invalidContent) {
        // Given
        UpdateCommentRequest request = new UpdateCommentRequest(invalidContent);

        // When
        Set<ConstraintViolation<UpdateCommentRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .hasSize(1)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("Content cannot be blank");
    }

    @ParameterizedTest(name = "Should fail validation for blank content: [{0}]")
    @MethodSource("provideBlankContentVariations")
    @DisplayName("Content validation - Blank Content Variations")
    void updateCommentRequest_BlankContent_FailsValidation(String blankContent) {
        // Given
        UpdateCommentRequest request = new UpdateCommentRequest(blankContent);

        // When
        Set<ConstraintViolation<UpdateCommentRequest>> violations = validator.validate(request);

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
    void updateCommentRequest_InvalidContent_ContainsCorrectFieldName() {
        // Given
        UpdateCommentRequest request = new UpdateCommentRequest(null);

        // When
        Set<ConstraintViolation<UpdateCommentRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .hasSize(1)
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("content");
    }

    // Test 4: Constructor and getter/setter testing
    @Test
    @DisplayName("Should correctly set and get values through constructors and accessors")
    void updateCommentRequest_ConstructorsAndAccessors_WorkCorrectly() {
        // Given
        String expectedContent = "Updated test comment content";

        // When - Test all-args constructor
        UpdateCommentRequest request1 = new UpdateCommentRequest(expectedContent);

        // Then
        assertThat(request1.getContent()).isEqualTo(expectedContent);

        // When - Test no-args constructor + setter
        UpdateCommentRequest request2 = new UpdateCommentRequest();
        request2.setContent(expectedContent);

        // Then
        assertThat(request2.getContent()).isEqualTo(expectedContent);

        // Verify both requests pass validation
        assertThat(validator.validate(request1)).isEmpty();
        assertThat(validator.validate(request2)).isEmpty();
    }

    // Test 5: Edge cases with special characters
    @Test
    @DisplayName("Should accept content with special characters and Unicode")
    void updateCommentRequest_SpecialCharactersInContent_PassesValidation() {
        // Given
        String[] specialContents = {
                "Updated comment with émojis 🔄 and ñice çharacters",
                "Updated comment with symbols: @#$%^&*()_+{}|:<>?",
                "Updated comment with\nnewlines\tand\ttabs",
                "Updated comment with \"quotes\" and 'apostrophes'",
                "Updated comment with [brackets] and (parentheses)",
                "Updated HTML-like content: <div>Hello Updated</div>",
                "Updated JSON-like: {\"message\": \"updated\"}",
                "UPDATED CONTENT IN UPPERCASE",
                "updated content in lowercase",
                "MiXeD cAsE uPdAtEd CoNtEnT",
                "Content with numbers: 123456789",
                "Content with special Unicode: ♠♣♥♦✓✗→←↑↓",
        };

        // When & Then
        for (String content : specialContents) {
            UpdateCommentRequest request = new UpdateCommentRequest(content);
            Set<ConstraintViolation<UpdateCommentRequest>> violations = validator.validate(request);

            assertThat(violations)
                    .as("Content '%s' should be valid", content)
                    .isEmpty();
        }
    }

    // Test 6: Boundary testing for whitespace trimming
    @Test
    @DisplayName("Should fail validation for content that becomes empty after trimming")
    void updateCommentRequest_ContentEmptyAfterTrimming_FailsValidation() {
        // Given - Content that only contains whitespace
        UpdateCommentRequest request = new UpdateCommentRequest("   \t\n   ");

        // When
        Set<ConstraintViolation<UpdateCommentRequest>> violations = validator.validate(request);

        // Then - @NotBlank should catch this
        assertThat(violations)
                .hasSize(1)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("Content cannot be blank");
    }

    // Test 7: Content with leading/trailing whitespace
    @Test
    @DisplayName("Should pass validation for content with leading/trailing whitespace")
    void updateCommentRequest_ContentWithLeadingTrailingWhitespace_PassesValidation() {
        // Given - Content with whitespace around valid text
        String[] contentsWithWhitespace = {
                "  updated content  ",      // Leading and trailing spaces
                "\tupdated content\t",      // Leading and trailing tabs
                "\nupdated content\n",      // Leading and trailing newlines
                "  \tupdated content\n  ",  // Mixed leading/trailing whitespace
        };

        // When & Then
        for (String content : contentsWithWhitespace) {
            UpdateCommentRequest request = new UpdateCommentRequest(content);
            Set<ConstraintViolation<UpdateCommentRequest>> violations = validator.validate(request);

            assertThat(violations)
                    .as("Content '%s' should be valid", content)
                    .isEmpty();
        }
    }

    // Test 8: Real-world update scenarios
    @Test
    @DisplayName("Should handle real-world comment update scenarios")
    void updateCommentRequest_RealWorldScenarios_PassValidation() {
        // Given - Real-world update scenarios
        String[] realWorldUpdates = {
                "Fixed typo in my original comment",
                "Added more details: the solution works correctly",
                "EDIT: Found the actual issue, it was a configuration problem",
                "Update: This approach didn't work, trying different solution",
                "Clarification: What I meant was...",
                "Thanks for the feedback! Updated my approach accordingly.",
                "Corrected the code snippet:\n``````",
                "Update after testing: This solution works in production",
        };

        // When & Then
        for (String update : realWorldUpdates) {
            UpdateCommentRequest request = new UpdateCommentRequest(update);
            Set<ConstraintViolation<UpdateCommentRequest>> violations = validator.validate(request);

            assertThat(violations)
                    .as("Update content '%s' should be valid", update)
                    .isEmpty();
        }
    }

    // Test 9: Error message consistency
    @Test
    @DisplayName("Should provide consistent error messages for all invalid content scenarios")
    void updateCommentRequest_AllInvalidScenarios_ProduceSameErrorMessage() {
        // Given - Different types of invalid content
        String[] invalidContents = {null, "", " ", "\t", "\n", "   "};
        String expectedMessage = "Content cannot be blank";

        // When & Then - All should produce the same error message
        for (String invalidContent : invalidContents) {
            UpdateCommentRequest request = new UpdateCommentRequest(invalidContent);
            Set<ConstraintViolation<UpdateCommentRequest>> violations = validator.validate(request);

            assertThat(violations)
                    .hasSize(1)
                    .extracting(ConstraintViolation::getMessage)
                    .containsExactly(expectedMessage);
        }
    }

    // Test 10: Multiple instantiation patterns
    @Test
    @DisplayName("Should handle various instantiation patterns correctly")
    void updateCommentRequest_VariousInstantiationPatterns_WorkCorrectly() {
        // Pattern 1: All-args constructor
        UpdateCommentRequest request1 = new UpdateCommentRequest("Updated content 1");

        // Pattern 2: No-args constructor with setter
        UpdateCommentRequest request2 = new UpdateCommentRequest();
        request2.setContent("Updated content 2");

        // Verify all patterns work and pass validation
        assertThat(validator.validate(request1)).isEmpty();
        assertThat(validator.validate(request2)).isEmpty();

        // Verify values are set correctly
        assertThat(request1.getContent()).isEqualTo("Updated content 1");
        assertThat(request2.getContent()).isEqualTo("Updated content 2");
    }

    // Test 11: Comparison with null content edge case
    @Test
    @DisplayName("Should handle null content explicitly set after construction")
    void updateCommentRequest_NullContentAfterConstruction_FailsValidation() {
        // Given
        UpdateCommentRequest request = new UpdateCommentRequest("Valid content");
        request.setContent(null); // Set to null after construction

        // When
        Set<ConstraintViolation<UpdateCommentRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .hasSize(1)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("Content cannot be blank");
    }
}
