package olsh.backend.api_gateway.service;

import olsh.backend.api_gateway.dto.request.CreateCommentRequest;
import olsh.backend.api_gateway.dto.request.GetCommentsRequest;
import olsh.backend.api_gateway.dto.response.CommentListResponse;
import olsh.backend.api_gateway.dto.response.CommentResponse;
import olsh.backend.api_gateway.dto.response.UserResponse;
import olsh.backend.api_gateway.exception.CommentNotFoundException;
import olsh.backend.api_gateway.exception.ForbiddenAccessException;
import olsh.backend.api_gateway.exception.LabNotFoundException;
import olsh.backend.api_gateway.grpc.client.CommentServiceClient;
import olsh.backend.api_gateway.grpc.proto.CommentProto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("CommentService - Business Logic and Validation Tests")
class CommentServiceTest {

    private CommentService commentService;
    private CommentServiceClient commentServiceClient;
    private LabService labService;
    private ArticleService articleService;
    private UserService userService;

    @BeforeEach
    @DisplayName("Set up test environment with mocked dependencies")
    void setUp() {
        commentServiceClient = mock(CommentServiceClient.class);
        labService = mock(LabService.class);
        userService = mock(UserService.class);
        articleService = mock(ArticleService.class);
        commentService = new CommentService(commentServiceClient, labService, articleService,  userService);
        System.out.println("Setting up CommentService tests...");
    }

    @AfterEach
    @DisplayName("Clean up test environment")
    void tearDown() {
        System.out.println("Cleaning up CommentService tests...");
        commentService = null;
    }

    @Test
    @DisplayName("Should allow user to delete their own comment")
    void deleteComment_OwnerDeletesComment_SuccessfullyDeletes() {
        // Given
        String commentId = "comment-123";
        long ownerId = 456L;

        CommentProto.Comment protoComment = createTestProtoComment(commentId, ownerId, 1L, "lab");
        when(commentServiceClient.getCommentById(any(CommentProto.GetCommentRequest.class))).thenReturn(protoComment);
        when(userService.getUserByIdSafe(ownerId)).thenReturn(createTestUser(ownerId));
        when(commentServiceClient.deleteComment(any(CommentProto.DeleteCommentRequest.class))).thenReturn(true);

        // When & Then - Should not throw exception
        boolean result = commentService.deleteComment(commentId, ownerId);

        // Verify
        assertThat(result).isTrue();
        verify(commentServiceClient).deleteComment(any(CommentProto.DeleteCommentRequest.class));
    }

    @Test
    @DisplayName("Should throw ForbiddenAccessException when user tries to delete another user's comment")
    void deleteComment_NonOwnerDeletesComment_ThrowsForbiddenAccessException() {
        // Given
        String commentId = "comment-123";
        long actualOwnerId = 456L;
        long attemptingUserId = 789L;

        CommentProto.Comment protoComment = createTestProtoComment(commentId, actualOwnerId, 1L, "lab");
        when(commentServiceClient.getCommentById(any(CommentProto.GetCommentRequest.class))).thenReturn(protoComment);
        when(userService.getUserByIdSafe(actualOwnerId)).thenReturn(createTestUser(actualOwnerId));

        // When & Then
        assertThatThrownBy(() -> commentService.deleteComment(commentId, attemptingUserId))
                .isInstanceOf(ForbiddenAccessException.class)
                .hasMessage("You can only delete your own comments");

        verify(commentServiceClient, never()).deleteComment(any(CommentProto.DeleteCommentRequest.class));
    }

    @Test
    @DisplayName("Should validate lab exists before creating comment")
    void createComment_ValidLab_PassesValidation() {
        // Given
        long validLabId = 1L;
        long userId = 123L;
        CreateCommentRequest request = new CreateCommentRequest("Test comment", "");

        CommentProto.Comment protoComment = createTestProtoComment("comment-123", userId, validLabId, "lab");
        when(commentServiceClient.createComment(any(CommentProto.CreateCommentRequest.class))).thenReturn(protoComment);
        when(userService.getUserByIdSafe(userId)).thenReturn(createTestUser(userId));

        // When
        CommentResponse result = commentService.createComment(validLabId, userId, request, "lab");

        // Then
        verify(labService).validateLabExists(validLabId);
        assertThat(result).isNotNull();
        assertThat(result.getContentId()).isEqualTo(validLabId);
    }

    @Test
    @DisplayName("Should throw LabNotFoundException when lab does not exist during comment creation")
    void createComment_NonExistentLab_ThrowsLabNotFoundException() {
        // Given
        long nonExistentLabId = 999L;
        long userId = 123L;
        CreateCommentRequest request = new CreateCommentRequest("Test comment", null);

        doThrow(new LabNotFoundException("Lab not found")).when(labService).validateLabExists(nonExistentLabId);

        // When & Then
        assertThatThrownBy(() -> commentService.createComment(nonExistentLabId, userId, request, "lab"))
                .isInstanceOf(LabNotFoundException.class)
                .hasMessage("Lab not found");

        verify(commentServiceClient, never()).createComment(any(CommentProto.CreateCommentRequest.class));
    }

    @Test
    @DisplayName("Should validate lab exists before getting lab comments")
    void getLabComments_ValidLab_PassesValidation() {
        // Given
        long validLabId = 1L;
        GetCommentsRequest request = new GetCommentsRequest(1, 20);

        CommentProto.ListCommentsResponse protoResponse = createTestProtoCommentListResponse();
        when(commentServiceClient.getComments(any(CommentProto.ListCommentsRequest.class))).thenReturn(protoResponse);
        when(userService.getUserByIdSafe(anyLong())).thenReturn(createTestUser(123L));

        // When
        CommentListResponse result = commentService.getLabComments(validLabId, request, "lab");

        // Then
        verify(labService).validateLabExists(validLabId);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Should enrich comment with user info when user exists")
    void enrichCommentWithUserInfo_UserExists_EnrichesCorrectly() {
        // Given
        String commentId = "comment-123";
        long userId = 123L;

        CommentProto.Comment protoComment = createTestProtoComment(commentId, userId, 1L, "lab");
        UserResponse userInfo = createTestUser(userId, "John", "Doe");

        when(commentServiceClient.getCommentById(any(CommentProto.GetCommentRequest.class))).thenReturn(protoComment);
        when(userService.getUserByIdSafe(userId)).thenReturn(userInfo);

        // When
        CommentResponse result = commentService.getCommentById(commentId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getFirstName()).isEqualTo("John");
        assertThat(result.getLastName()).isEqualTo("Doe");
    }

    @Test
    @DisplayName("Should handle CommentNotFoundException when trying to delete non-existent comment")
    void deleteComment_NonExistentComment_PropagatesCommentNotFoundException() {
        // Given
        String nonExistentCommentId = "non-existent-comment";
        long userId = 123L;

        when(commentServiceClient.getCommentById(any(CommentProto.GetCommentRequest.class)))
                .thenThrow(new CommentNotFoundException("Comment not found"));

        // When & Then
        assertThatThrownBy(() -> commentService.deleteComment(nonExistentCommentId, userId))
                .isInstanceOf(CommentNotFoundException.class)
                .hasMessage("Comment not found");

        verify(commentServiceClient, never()).deleteComment(any(CommentProto.DeleteCommentRequest.class));
    }

    // Helper methods to create test data
    private CommentProto.Comment createTestProtoComment(String id, long userId, long labId, String type) {
        return CommentProto.Comment.newBuilder()
                .setId(id)
                .setContentId(labId)
                .setUserId(userId)
                .setContent("Test comment content")
                .setType(type)
                .setCreatedAt(com.google.protobuf.Timestamp.newBuilder().setSeconds(1704067200).build())
                .setUpdatedAt(com.google.protobuf.Timestamp.newBuilder().setSeconds(1704067200).build())
                .build();
    }

    private UserResponse createTestUser(long userId) {
        return createTestUser(userId, "John", "Doe");
    }

    private UserResponse createTestUser(long userId, String firstName, String lastName) {
        return new UserResponse(userId, "User", firstName, lastName, null, 0, 0, 0);
    }

    private CommentProto.ListCommentsResponse createTestProtoCommentListResponse() {
        return CommentProto.ListCommentsResponse.newBuilder()
                .addComments(createTestProtoComment("comment-1", 123L, 1L, "lab"))
                .addComments(createTestProtoComment("comment-2", 456L, 1L, "lab"))
                .setTotalCount(2)
                .build();
    }

    // Data provider for user ID mismatch scenarios
    static Stream<org.junit.jupiter.params.provider.Arguments> provideUserIdMismatchScenarios() {
        return Stream.of(
                org.junit.jupiter.params.provider.Arguments.of(123L, 456L),
                org.junit.jupiter.params.provider.Arguments.of(1L, 999L),
                org.junit.jupiter.params.provider.Arguments.of(100L, 1L),
                org.junit.jupiter.params.provider.Arguments.of(789L, 321L)
        );
    }
}

