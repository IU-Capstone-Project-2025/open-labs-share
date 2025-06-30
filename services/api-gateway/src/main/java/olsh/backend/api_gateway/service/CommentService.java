package olsh.backend.api_gateway.service;

import olsh.backend.api_gateway.dto.request.GetCommentsRequest;
import olsh.backend.api_gateway.dto.request.UpdateCommentRequest;
import olsh.backend.api_gateway.dto.response.CommentListResponse;
import olsh.backend.api_gateway.dto.response.CommentResponse;
import olsh.backend.api_gateway.dto.response.UserResponse;
import olsh.backend.api_gateway.exception.ForbiddenAccessException;
import olsh.backend.api_gateway.exception.LabNotFoundException;
import olsh.backend.api_gateway.exception.UserNotFoundException;
import olsh.backend.api_gateway.grpc.client.CommentServiceClient;
import olsh.backend.api_gateway.dto.request.CreateCommentRequest;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentServiceClient commentServiceClient;
    private final LabService labService;
    private final UserService userService;

    public CommentResponse createComment(long labId, long userId, CreateCommentRequest request) {
        log.debug("Creating comment for lab ID: {} by user ID: {}", labId, userId);
        validateLabExists(labId);
        CommentResponse comment = commentServiceClient.createComment(labId, userId, request);
        return enrichCommentWithUserInfo(comment);
    }

    public CommentResponse getCommentById(String commentId) {
        CommentResponse comment = commentServiceClient.getCommentById(commentId);
        return enrichCommentWithUserInfo(comment);
    }

    public CommentListResponse getLabComments(long labId, GetCommentsRequest request) {
        validateLabExists(labId);
        CommentListResponse response = commentServiceClient.getLabComments(labId, request);
        return enrichCommentsWithUserInfo(response);
    }

    public CommentListResponse getCommentReplies(String commentId, GetCommentsRequest request) {
        CommentListResponse response = commentServiceClient.getCommentReplies(commentId, request);
        return enrichCommentsWithUserInfo(response);
    }

    public CommentResponse updateComment(String commentId, long userId, UpdateCommentRequest request) {
        CommentResponse oldComment = getCommentById(commentId);
        if (userId != oldComment.getUserId()){
            log.warn("User {} attempted to update comment {} owned by user {}",
                    userId, commentId, oldComment.getUserId());
            throw new ForbiddenAccessException("Only author can update the comment");
        }
        CommentResponse comment = commentServiceClient.updateComment(commentId, request);
        return enrichCommentWithUserInfo(comment);
    }

    public void deleteComment(String commentId, long userId) {
        log.debug("Attempting to delete comment ID: {} by user ID: {}", commentId, userId);
        
        CommentResponse comment = getCommentById(commentId);
        if (comment.getUserId() != userId) {
            log.warn("User {} attempted to delete comment {} owned by user {}", 
                    userId, commentId, comment.getUserId());
            throw new ForbiddenAccessException("You can only delete your own comments");
        }

        log.debug("Authorization check passed, deleting comment ID: {}", commentId);
        commentServiceClient.deleteComment(commentId);
        log.debug("Successfully deleted comment ID: {}", commentId);
    }

    private void validateLabExists(long labId) {
        try {
            labService.getLabById(labId);
        } catch (LabNotFoundException e) {
            log.error("Cannot create comment action for non-existent lab with ID: {}", labId);
            throw e;
        }
    }

    private CommentResponse enrichCommentWithUserInfo(CommentResponse comment) {
        UserResponse user;
        try {
            user = userService.getUserById(comment.getUserId());
        } catch (UserNotFoundException e) {
            log.warn("User not found for comment ID: {}, user ID: {}. Using empty user info.", 
                    comment.getId(), comment.getUserId());
            // If user is not found, we still return the comment but with empty user info
            user = new UserResponse(
                    comment.getUserId(),
                    "User",
                    "Unknown",
                    "Unknown",
                    null
            );
        }

        return CommentResponse.builder()
                .id(comment.getId())
                .labId(comment.getLabId())
                .userId(comment.getUserId())
                .firstName(user.getName())
                .lastName(user.getSurname())
                .parentId(comment.getParentId())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }

    private CommentListResponse enrichCommentsWithUserInfo(CommentListResponse response) {
        // Create a cache for user responses to avoid multiple calls for the same user
        HashMap<Long, UserResponse> userCache = new HashMap<>();

        var enrichedComments = response.getComments().stream()
                .map(comment -> {
                    UserResponse user = userCache.computeIfAbsent(comment.getUserId(), userId -> {
                        try {
                            return userService.getUserById(userId);
                        } catch (UserNotFoundException e) {
                            log.warn("User not found for comment ID: {}, user ID: {}. Using empty user info.", 
                                    comment.getId(), userId);
                            return new UserResponse(
                                    userId,
                                    "unknown",
                                    "Unknown",
                                    "User",
                                    null
                            );
                        }
                    });
                    
                    return CommentResponse.builder()
                            .id(comment.getId())
                            .labId(comment.getLabId())
                            .userId(comment.getUserId())
                            .firstName(user.getName())
                            .lastName(user.getSurname())
                            .parentId(comment.getParentId())
                            .content(comment.getContent())
                            .createdAt(comment.getCreatedAt())
                            .updatedAt(comment.getUpdatedAt())
                            .build();
                })
                .toList();

        return CommentListResponse.builder()
                .comments(enrichedComments)
                .pagination(response.getPagination())
                .build();
    }
} 