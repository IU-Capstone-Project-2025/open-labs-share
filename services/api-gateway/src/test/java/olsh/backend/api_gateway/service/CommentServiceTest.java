package olsh.backend.api_gateway.service;

import olsh.backend.api_gateway.dto.request.CreateCommentRequest;
import olsh.backend.api_gateway.dto.request.GetCommentsRequest;
import olsh.backend.api_gateway.dto.response.CommentListResponse;
import olsh.backend.api_gateway.dto.response.CommentResponse;
import olsh.backend.api_gateway.dto.response.UserResponse;
import olsh.backend.api_gateway.exception.CommentNotFoundException;
import olsh.backend.api_gateway.exception.ForbiddenAccessException;
import olsh.backend.api_gateway.exception.LabNotFoundException;
import olsh.backend.api_gateway.exception.UserNotFoundException;
import olsh.backend.api_gateway.grpc.client.CommentServiceClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;


@DisplayName("CommentService - Business Logic and Validation Tests")
class CommentServiceTest {

    private CommentService commentService;
    private CommentServiceClient commentServiceClient;
    private LabService labService;
    private UserService userService;

    @BeforeEach
    @DisplayName("Set up test environment with mocked dependencies")
    void setUp() {
        // Create mocks for dependencies
        commentServiceClient = mock(CommentServiceClient.class);
        labService = mock(LabService.class);
        userService = mock(UserService.class);

        // Create REAL service instance
        commentService = new CommentService(commentServiceClient, labService, userService);

        System.out.println("Setting up CommentService tests...");
    }

    @AfterEach
    @DisplayName("Clean up test environment")
    void tearDown() {
        System.out.println("Cleaning up CommentService tests...");
        commentService = null;
    }

    // Test 1: Delete comment ownership validation
    @Test
    @DisplayName("Should allow user to delete their own comment")
    void deleteComment_OwnerDeletesComment_SuccessfullyDeletes() {
        // Given
        String commentId = "comment-123";
        long ownerId = 456L;

        CommentResponse ownerComment = createTestComment(commentId, ownerId, 1L);
        when(commentServiceClient.getCommentById(commentId)).thenReturn(ownerComment);
        when(userService.getUserByIdSafe(ownerId)).thenReturn(createTestUser(ownerId));

        // When & Then - Should not throw exception
        commentService.deleteComment(commentId, ownerId);

        // Verify the deletion was called
        verify(commentServiceClient).deleteComment(commentId);
    }

    @Test
    @DisplayName("Should throw ForbiddenAccessException when user tries to delete another user's comment")
    void deleteComment_NonOwnerDeletesComment_ThrowsForbiddenAccessException() {
        // Given
        String commentId = "comment-123";
        long actualOwnerId = 456L;
        long attemptingUserId = 789L; // Different user

        CommentResponse ownerComment = createTestComment(commentId, actualOwnerId, 1L);
        when(commentServiceClient.getCommentById(commentId)).thenReturn(ownerComment);
        when(userService.getUserByIdSafe(actualOwnerId)).thenReturn(createTestUser(actualOwnerId));

        // When & Then
        assertThatThrownBy(() -> commentService.deleteComment(commentId, attemptingUserId))
                .isInstanceOf(ForbiddenAccessException.class)
                .hasMessage("You can only delete your own comments");

        // Verify deletion was NOT called
        verify(commentServiceClient, never()).deleteComment(commentId);
    }

    @Test
    @DisplayName("Should handle CommentNotFoundException when trying to delete non-existent comment")
    void deleteComment_NonExistentComment_PropagatesCommentNotFoundException() {
        // Given
        String nonExistentCommentId = "non-existent-comment";
        long userId = 123L;

        when(commentServiceClient.getCommentById(nonExistentCommentId))
                .thenThrow(new CommentNotFoundException("Comment not found"));

        // When & Then
        assertThatThrownBy(() -> commentService.deleteComment(nonExistentCommentId, userId))
                .isInstanceOf(CommentNotFoundException.class)
                .hasMessage("Comment not found");

        // Verify deletion was NOT called
        verify(commentServiceClient, never()).deleteComment(nonExistentCommentId);
    }

    // Test 2: Lab validation scenarios
    @Test
    @DisplayName("Should validate lab exists before creating comment")
    void createComment_ValidLab_PassesValidation() {
        // Given
        long validLabId = 1L;
        long userId = 123L;
        CreateCommentRequest request = new CreateCommentRequest("Test comment", null);

        // Mock lab validation to pass
        CommentResponse mockComment = createTestComment("comment-123", userId, validLabId);
        when(commentServiceClient.createComment(validLabId, userId, request)).thenReturn(mockComment);
        when(userService.getUserByIdSafe(userId)).thenReturn(createTestUser(userId));

        // When & Then - Should not throw exception during lab validation
        CommentResponse result = commentService.createComment(validLabId, userId, request);

        // Verify lab validation was called
        verify(labService).getLabById(validLabId);
        assertThat(result).isNotNull();
        assertThat(result.getLabId()).isEqualTo(validLabId);
    }

    @Test
    @DisplayName("Should throw LabNotFoundException when lab does not exist during comment creation")
    void createComment_NonExistentLab_ThrowsLabNotFoundException() {
        // Given
        long nonExistentLabId = 999L;
        long userId = 123L;
        CreateCommentRequest request = new CreateCommentRequest("Test comment", null);

        // Mock lab validation to fail
        doThrow(new LabNotFoundException("Lab not found")).when(labService).getLabById(nonExistentLabId);

        // When & Then
        assertThatThrownBy(() -> commentService.createComment(nonExistentLabId, userId, request))
                .isInstanceOf(LabNotFoundException.class)
                .hasMessage("Lab not found");

        // Verify comment creation was NOT called
        verify(commentServiceClient, never()).createComment(anyLong(), anyLong(), any(CreateCommentRequest.class));
    }

    @Test
    @DisplayName("Should validate lab exists before getting lab comments")
    void getLabComments_ValidLab_PassesValidation() {
        // Given
        long validLabId = 1L;
        GetCommentsRequest request = new GetCommentsRequest(1, 20);

        // Mock lab validation to pass
        CommentListResponse mockResponse = createTestCommentListResponse();
        when(commentServiceClient.getComments(validLabId, request)).thenReturn(mockResponse);
        when(userService.getUserByIdSafe(anyLong())).thenReturn(createTestUser(123L));

        // When
        CommentListResponse result = commentService.getLabComments(validLabId, request);

        // Then
        verify(labService).getLabById(validLabId);
        assertThat(result).isNotNull();
    }

    // Test 3: User enrichment with error handling
    @Test
    @DisplayName("Should enrich comment with user info when user exists")
    void enrichCommentWithUserInfo_UserExists_EnrichesCorrectly() {
        // Given
        String commentId = "comment-123";
        long userId = 123L;

        CommentResponse commentWithoutUserInfo = createTestComment(commentId, userId, 1L);
        UserResponse userInfo = createTestUser(userId, "John", "Doe");

        when(commentServiceClient.getCommentById(commentId)).thenReturn(commentWithoutUserInfo);
        when(userService.getUserByIdSafe(userId)).thenReturn(userInfo);

        // When
        CommentResponse result = commentService.getCommentById(commentId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getFirstName()).isEqualTo("John");
        assertThat(result.getLastName()).isEqualTo("Doe");
    }

    // Test 4: Parameterized tests for invalid user IDs in ownership validation
    @ParameterizedTest(name = "Should throw ForbiddenAccessException when user {0} tries to delete comment owned by user {1}")
    @MethodSource("provideUserIdMismatchScenarios")
    @DisplayName("Ownership validation with various user ID scenarios")
    void deleteComment_UserIdMismatch_ThrowsForbiddenAccessException(long commentOwnerId, long attemptingUserId) {
        // Given
        String commentId = "comment-123";

        CommentResponse ownerComment = createTestComment(commentId, commentOwnerId, 1L);
        when(commentServiceClient.getCommentById(commentId)).thenReturn(ownerComment);
        when(userService.getUserByIdSafe(commentOwnerId)).thenReturn(createTestUser(commentOwnerId));

        // When & Then
        assertThatThrownBy(() -> commentService.deleteComment(commentId, attemptingUserId))
                .isInstanceOf(ForbiddenAccessException.class)
                .hasMessage("You can only delete your own comments");
    }

    // Data provider for user ID mismatch scenarios
    static Stream<org.junit.jupiter.params.provider.Arguments> provideUserIdMismatchScenarios() {
        return Stream.of(
                org.junit.jupiter.params.provider.Arguments.of(123L, 456L), // Different positive IDs
                org.junit.jupiter.params.provider.Arguments.of(1L, 999L),   // Small vs large ID
                org.junit.jupiter.params.provider.Arguments.of(100L, 1L),   // Large vs small ID
                org.junit.jupiter.params.provider.Arguments.of(789L, 321L)  // Another mismatch
        );
    }

    // Test 5: Parameterized tests for invalid comment/lab IDs
    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "null-comment", "invalid-id-123"})
    @DisplayName("Should handle various invalid comment ID formats")
    void deleteComment_InvalidCommentIds_PropagatesNotFoundException(String invalidCommentId) {
        // Given
        long userId = 123L;

        when(commentServiceClient.getCommentById(invalidCommentId))
                .thenThrow(new CommentNotFoundException("Comment not found"));

        // When & Then
        assertThatThrownBy(() -> commentService.deleteComment(invalidCommentId, userId))
                .isInstanceOf(CommentNotFoundException.class);
    }

    // Test 6: Exception message consistency
    @Test
    @DisplayName("Should provide consistent error messages for ownership violations")
    void deleteComment_OwnershipViolations_ConsistentErrorMessages() {
        // Given
        String[] commentIds = {"comment-1", "comment-2", "comment-3"};
        long ownerId = 100L;
        long intruderId = 200L;
        String expectedMessage = "You can only delete your own comments";

        // When & Then
        for (String commentId : commentIds) {
            CommentResponse ownerComment = createTestComment(commentId, ownerId, 1L);
            when(commentServiceClient.getCommentById(commentId)).thenReturn(ownerComment);
            when(userService.getUserByIdSafe(ownerId)).thenReturn(createTestUser(ownerId));

            assertThatThrownBy(() -> commentService.deleteComment(commentId, intruderId))
                    .isInstanceOf(ForbiddenAccessException.class)
                    .hasMessage(expectedMessage);
        }
    }

    // Test 7: Edge cases for user enrichment
    @Test
    @DisplayName("Should handle null parent ID correctly in comment enrichment")
    void enrichCommentWithUserInfo_NullParentId_HandlesCorrectly() {
        // Given
        CommentResponse commentWithNullParent = CommentResponse.builder()
                .id("comment-123")
                .labId(1L)
                .userId(123L)
                .parentId(null) // Null parent ID
                .content("Test comment")
                .createdAt("2024-01-01T00:00:00Z")
                .updatedAt("2024-01-01T00:00:00Z")
                .build();

        UserResponse userInfo = createTestUser(123L);

        when(commentServiceClient.getCommentById("comment-123")).thenReturn(commentWithNullParent);
        when(userService.getUserByIdSafe(123L)).thenReturn(userInfo);

        // When
        CommentResponse result = commentService.getCommentById("comment-123");

        // Then
        assertThat(result.getParentId()).isNull();
        assertThat(result.getFirstName()).isEqualTo("John");
        assertThat(result.getLastName()).isEqualTo("Doe");
    }

    // Helper methods to create test data
    private CommentResponse createTestComment(String id, long userId, long labId) {
        return CommentResponse.builder()
                .id(id)
                .labId(labId)
                .userId(userId)
                .content("Test comment content")
                .createdAt("2024-01-01T00:00:00Z")
                .updatedAt("2024-01-01T00:00:00Z")
                .parentId(null)
                .build();
    }

    private UserResponse createTestUser(long userId) {
        return createTestUser(userId, "John", "Doe");
    }

    private UserResponse createTestUser(long userId, String firstName, String lastName) {
        return new UserResponse(userId, "User", firstName, lastName, null, 0, 0, 0);
    }

    private CommentListResponse createTestCommentListResponse() {
        CommentResponse comment1 = createTestComment("comment-1", 123L, 1L);
        CommentResponse comment2 = createTestComment("comment-2", 456L, 1L);

        return CommentListResponse.builder()
                .comments(List.of(comment1, comment2))
                .pagination(CommentListResponse.PaginationResponse.builder()
                        .currentPage(1)
                        .totalItems(2)
                        .totalPages(1)
                        .build())
                .build();
    }
}

