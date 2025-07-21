package olsh.backend.api_gateway.service;

import olsh.backend.api_gateway.dto.request.GetCommentsRequest;
import olsh.backend.api_gateway.dto.request.UpdateCommentRequest;
import olsh.backend.api_gateway.dto.response.CommentListResponse;
import olsh.backend.api_gateway.dto.response.CommentResponse;
import olsh.backend.api_gateway.dto.response.UserResponse;
import olsh.backend.api_gateway.exception.ContentNotFoundException;
import olsh.backend.api_gateway.exception.ForbiddenAccessException;
import olsh.backend.api_gateway.exception.LabNotFoundException;
import olsh.backend.api_gateway.grpc.client.CommentServiceClient;
import olsh.backend.api_gateway.grpc.proto.CommentProto;
import olsh.backend.api_gateway.dto.request.CreateCommentRequest;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentServiceClient commentServiceClient;
    private final LabService labService;
    private final ArticleService articleService;
    private final UserService userService;

    /**
     * Creates a new comment for a lab.
     *
     * @param contentId the ID of the lab
     * @param userId the ID of the user creating the comment
     * @param request the request containing comment details
     * @return the created comment response
     */
    public CommentResponse createComment(Long contentId, Long userId, CreateCommentRequest request, String type) {
        validateContentExists(contentId, type);
        CommentProto.CreateCommentRequest grpcRequest = CommentProto.CreateCommentRequest.newBuilder()
                .setContentId(contentId)
                .setUserId(userId)
                .setContent(request.getContent())
                .setParentId(request.getParentId())
                .setType(type)
                .build();
        CommentProto.Comment comment = commentServiceClient.createComment(grpcRequest);
        CommentResponse response = mapCommentToResponse(comment);
        enrichCommentWithUserInfo(response);
        log.debug("Comment created successfully for lab ID: {} by user ID: {}", contentId, userId);
        return response;
    }

    /**
     * Retrieves a comment by its ID.
     *
     * @param commentId the ID of the comment
     * @return the comment response
     */
    public CommentResponse getCommentById(String commentId) {
        CommentProto.GetCommentRequest grpcRequest = CommentProto.GetCommentRequest.newBuilder()
                .setId(commentId)
                .build();
        CommentProto.Comment comment = commentServiceClient.getCommentById(grpcRequest);
        CommentResponse response = mapCommentToResponse(comment);
        enrichCommentWithUserInfo(response);
        log.debug("Fetched comment by ID: {}", commentId);
        return response;
    }

    /**
     * Retrieves comments for a specific lab.
     *
     * @param contentId the ID of the lab
     * @param request the request containing pagination details
     * @return the list of comments for the lab
     */
    public CommentListResponse getLabComments(long contentId, GetCommentsRequest request, String type) {
        validateContentExists(contentId, type);
        CommentProto.ListCommentsRequest grpcRequest = CommentProto.ListCommentsRequest.newBuilder()
                .setContentId(contentId)
                .setPage(request.getPage())
                .setLimit(request.getLimit())
                .setType(type)
                .build();
        CommentProto.ListCommentsResponse grpcResponse = commentServiceClient.getComments(grpcRequest);
        CommentListResponse response = mapCommentsToResponse(grpcResponse.getCommentsList());
        enrichCommentsWithUserInfo(response);
        log.debug("Fetched comments for lab ID: {} on page: {}, limit: {}", contentId, request.getPage(),
                request.getLimit());
        return response;
    }

    /**
     * Retrieves replies for a specific comment.
     *
     * @param commentId the ID of the comment
     * @param request the request containing pagination details
     * @return the list of replies for the comment
     */
    public CommentListResponse getCommentReplies(String commentId, GetCommentsRequest request) {
        CommentProto.GetCommentRepliesRequest grpcRequest = CommentProto.GetCommentRepliesRequest.newBuilder()
                .setCommentId(commentId)
                .setPage(request.getPage())
                .setLimit(request.getLimit())
                .build();
        CommentProto.GetCommentRepliesResponse grpcResponse = commentServiceClient.getCommentReplies(grpcRequest);
        CommentListResponse response = mapCommentsToResponse(grpcResponse.getCommentsList());
        enrichCommentsWithUserInfo(response);
        log.debug("Fetched replies for comment ID: {} on page: {}, limit: {}", commentId, request.getPage(),
                request.getLimit());
        return response;
    }

    /**
     * Updates an existing comment.
     *
     * @param commentId the ID of the comment to update
     * @param userId the ID of the user updating the comment
     * @param request the request containing updated comment details
     * @return the updated comment response
     */
    public CommentResponse updateComment(String commentId, long userId, UpdateCommentRequest request) {
        CommentResponse oldComment = getCommentById(commentId);
        if (userId != oldComment.getUserId()) {
            log.warn("User {} attempted to update comment {} owned by user {}",
                    userId, commentId, oldComment.getUserId());
            throw new ForbiddenAccessException("Only author can update the comment");
        }
        CommentProto.UpdateCommentRequest grpcRequest = CommentProto.UpdateCommentRequest.newBuilder()
                .setId(commentId)
                .setUserId(userId)
                .setContent(request.getContent())
                .build();
        CommentProto.Comment comment = commentServiceClient.updateComment(grpcRequest);
        CommentResponse response = mapCommentToResponse(comment);
        enrichCommentWithUserInfo(response);
        log.debug("Comment ID: {} updated successfully by user ID: {}", commentId, userId);
        return response;
    }

    /**
     * Deletes a comment.
     *
     * @param commentId the ID of the comment to delete
     * @param userId the ID of the user deleting the comment
     * @return true if deletion was successful, false otherwise
     */
    public boolean deleteComment(String commentId, long userId) {
        log.debug("Attempting to delete comment ID: {} by user ID: {}", commentId, userId);
        CommentResponse comment = getCommentById(commentId);
        if (comment.getUserId() != userId) {
            log.warn("User {} attempted to delete comment {} owned by user {}",
                    userId, commentId, comment.getUserId());
            throw new ForbiddenAccessException("You can only delete your own comments");
        }
        CommentProto.DeleteCommentRequest grpcRequest = CommentProto.DeleteCommentRequest.newBuilder()
                .setUserId(userId)
                .setId(commentId)
                .build();
        boolean success = commentServiceClient.deleteComment(grpcRequest);
        log.debug("Deleted comment ID: {} with success: {}", commentId, success);
        return success;
    }

    private void validateContentExists(long id, String type) {
        try {
            switch (type) {
                case "lab": labService.validateLabExists(id);
                break;
                case "article": articleService.validateArticleExists(id);
                break;
                default: throw new ContentNotFoundException(
                        String.format("Couldn't find the content of type %s with id %d", type, id ));
            }
        } catch (ContentNotFoundException e) {
            log.error("Cannot create comment action for non-existent lab with ID: {}", id);
            throw e;
        }
    }

    /**
     * Maps a CommentProto.Comment to CommentResponse.
     *
     * @param Comment the gRPC comment object
     * @return the mapped CommentResponse
     */
    private CommentResponse mapCommentToResponse(CommentProto.Comment Comment) {
        return CommentResponse.builder()
                .id(Comment.getId())
                .contentId(Comment.getContentId())
                .userId(Comment.getUserId())
                .parentId(Comment.hasParentId() ? Comment.getParentId() : null)
                .content(Comment.getContent())
                .createdAt(TimestampConverter.convertTimestampToIso(Comment.getCreatedAt()))
                .updatedAt(TimestampConverter.convertTimestampToIso(Comment.getUpdatedAt()))
                .build();
    }

    /**
     * Maps a list of CommentProto.Comment to CommentListResponse.
     *
     * @param list the list of gRPC comments
     * @return the mapped CommentListResponse
     */
    private CommentListResponse mapCommentsToResponse(List<CommentProto.Comment> list) {
        var comments = list.stream()
                .map(this::mapCommentToResponse)
                .toList();
        return CommentListResponse.builder()
                .comments(comments)
                .count(comments.size())
                .build();
    }

    /**
     * Enriches a single comment with user information.
     *
     * @param comment the comment to enrich
     */
    private void enrichCommentWithUserInfo(CommentResponse comment) {
        UserResponse user = userService.getUserByIdSafe(comment.getUserId());
        comment.setFirstName(user.getName());
        comment.setLastName(user.getSurname());
    }

    /**
     * Enriches a list of comments with user information.
     *
     * @param response the response containing the list of comments
     */
    private void enrichCommentsWithUserInfo(CommentListResponse response) {
        HashMap<Long, UserResponse> cache = new HashMap<>();
        response.getComments().forEach(comment -> {
            UserResponse user = cache.computeIfAbsent(comment.getUserId(), userService::getUserByIdSafe);
            comment.setFirstName(user.getName());
            comment.setLastName(user.getSurname());
        });
    }
} 