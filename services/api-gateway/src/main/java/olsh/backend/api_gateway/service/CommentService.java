package olsh.backend.api_gateway.service;

import olsh.backend.api_gateway.dto.request.GetCommentsRequest;
import olsh.backend.api_gateway.dto.request.UpdateCommentRequest;
import olsh.backend.api_gateway.dto.response.CommentListResponse;
import olsh.backend.api_gateway.dto.response.CommentResponse;
import olsh.backend.api_gateway.dto.response.UserResponse;
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
    private final UserService userService;

    public CommentResponse createComment(Long labId, Long userId, CreateCommentRequest request) {
        validateLabExists(labId);
        CommentProto.CreateCommentRequest grpcRequest = CommentProto.CreateCommentRequest.newBuilder()
                .setContentId(labId)
                .setUserId(userId)
                .setContent(request.getContent())
                .setParentId(request.getParentId())
                .build();
        CommentProto.Comment comment = commentServiceClient.createComment(grpcRequest);
        CommentResponse response = mapCommentToResponse(comment);
        enrichCommentWithUserInfo(response);
        log.debug("Comment created successfully for lab ID: {} by user ID: {}", labId, userId);
        return response;
    }

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

    public CommentListResponse getLabComments(long labId, GetCommentsRequest request) {
        validateLabExists(labId);
        CommentProto.ListCommentsRequest grpcRequest = CommentProto.ListCommentsRequest.newBuilder()
                .setContentId(labId)
                .setPage(request.getPage())
                .setLimit(request.getLimit())
                .build();
        CommentProto.ListCommentsResponse grpcResponse = commentServiceClient.getComments(grpcRequest);
        CommentListResponse response = mapCommentsToResponse(grpcResponse.getCommentsList(),
                grpcResponse.getTotalCount(), request.getPage());
        enrichCommentsWithUserInfo(response);
        log.debug("Fetched comments for lab ID: {} on page: {}, limit: {}", labId, request.getPage(),
                request.getLimit());
        return response;
    }

    public CommentListResponse getCommentReplies(String commentId, GetCommentsRequest request) {
        CommentProto.GetCommentRepliesRequest grpcRequest = CommentProto.GetCommentRepliesRequest.newBuilder()
                .setCommentId(commentId)
                .setPage(request.getPage())
                .setLimit(request.getLimit())
                .build();
        CommentProto.GetCommentRepliesResponse grpcResponse = commentServiceClient.getCommentReplies(grpcRequest);
        CommentListResponse response = mapCommentsToResponse(grpcResponse.getCommentsList(),
                grpcResponse.getTotalCount(), request.getPage());
        enrichCommentsWithUserInfo(response);
        log.debug("Fetched replies for comment ID: {} on page: {}, limit: {}", commentId, request.getPage(),
                request.getLimit());
        return response;
    }

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

    private void validateLabExists(long labId) {
        try {
            labService.getLabById(labId);
        } catch (LabNotFoundException e) {
            log.error("Cannot create comment action for non-existent lab with ID: {}", labId);
            throw e;
        }
    }

    private CommentResponse mapCommentToResponse(CommentProto.Comment Comment) {
        return CommentResponse.builder()
                .id(Comment.getId())
                .labId(Comment.getContentId())
                .userId(Comment.getUserId())
                .parentId(Comment.hasParentId() ? Comment.getParentId() : null)
                .content(Comment.getContent())
                .createdAt(TimestampConverter.convertTimestampToIso(Comment.getCreatedAt()))
                .updatedAt(TimestampConverter.convertTimestampToIso(Comment.getUpdatedAt()))
                .build();
    }

    private CommentListResponse mapCommentsToResponse(List<CommentProto.Comment> list, int totalCount, int page) {
        var comments = list.stream()
                .map(this::mapCommentToResponse)
                .toList();
        return CommentListResponse.builder()
                .comments(comments)
                .pagination(CommentListResponse.PaginationResponse.builder()
                        .currentPage(page)
                        .totalItems(totalCount)
                        .totalPages(0)
                        .build())
                .build();
    }

    private void enrichCommentWithUserInfo(CommentResponse comment) {
        UserResponse user = userService.getUserByIdSafe(comment.getUserId());
        comment.setFirstName(user.getName());
        comment.setLastName(user.getSurname());
    }

    private void enrichCommentsWithUserInfo(CommentListResponse response) {
        HashMap<Long, UserResponse> cache = new HashMap<>();
        response.getComments().forEach(comment -> {
            UserResponse user = cache.computeIfAbsent(comment.getUserId(), userService::getUserByIdSafe);
            comment.setFirstName(user.getName());
            comment.setLastName(user.getSurname());
        });
    }
} 