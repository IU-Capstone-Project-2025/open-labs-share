package olsh.backend.api_gateway.grpc.client;

import com.google.protobuf.Timestamp;
import io.grpc.Channel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import olsh.backend.api_gateway.exception.CommentNotFoundException;
import olsh.backend.api_gateway.grpc.proto.CommentServiceGrpc;
import olsh.backend.api_gateway.grpc.proto.CommentProto.*;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class CommentServiceClient {

    private final CommentServiceGrpc.CommentServiceBlockingStub commentBlockingStub;

    public CommentServiceClient(GrpcChannelFactory channelFactory) {
        Channel channel = channelFactory.createChannel("feedback-service");
        this.commentBlockingStub = CommentServiceGrpc.newBlockingStub(channel);
    }

    /**
     * Creates a new comment for a specific content item.
     *
     * @param request Contains content ID, user ID, and comment text
     * @return Created Comment object with all details
     */
    public Comment createComment(CreateCommentRequest request) {
        try {
            log.debug("Creating comment for content ID: {} by user ID: {}", request.getContentId(),
                    request.getUserId());
            Comment comment = commentBlockingStub.createComment(request);
            log.debug("Comment created with ID: {}", comment.getId());
            return comment;
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.INVALID_ARGUMENT) {
                throw new IllegalArgumentException(e.getStatus().getDescription());
            } else {
                // For UNAVAILABLE, INTERNAL, or other unexpected errors, throw a generic 500-level exception.
                throw new RuntimeException("gRPC call to feedback-service failed", e);
            }
        }
    }

    /**
     * Retrieves a comment by its ID.
     *
     * @param request Contains the ID of the comment to retrieve
     * @return Comment object with all details
     * @throws CommentNotFoundException if the comment does not exist
     */
    public Comment getCommentById(GetCommentRequest request) {
        try {
            log.debug("Fetching comment with ID: {}", request.getId());
            Comment grpcResponse = commentBlockingStub.getComment(request);
            log.debug("Comment fetched successfully: {}", grpcResponse);
            return grpcResponse;
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
                throw new CommentNotFoundException("Comment with id " + request.getId() + " not found");
            } else {
                // For UNAVAILABLE, INTERNAL, or other unexpected errors
                throw new RuntimeException("gRPC call to feedback-service failed while getting comment", e);
            }
        }
    }

    /**
     * Lists comments for a specific content item with pagination.
     *
     * @param request Contains content ID, page number, and limit
     * @return ListCommentsResponse containing comments and total count
     */
    public ListCommentsResponse getComments(ListCommentsRequest request) {
        try {
            log.debug("Listing comments for content ID: {} on page: {}, limit: {}", request.getContentId(),
                    request.getPage(), request.getLimit());
            ListCommentsResponse response = commentBlockingStub.listComments(request);
            log.debug("Comments listed successfully, total count: {}", response.getTotalCount());
            return response;
        } catch (StatusRuntimeException e) {
            // For UNAVAILABLE, INTERNAL, or other unexpected errors
            throw new RuntimeException("gRPC call to feedback-service failed while listing lab comments", e);
        }
    }

    /**
     * Lists replies for a specific comment with pagination.
     *
     * @param request Contains comment ID, page number, and limit
     * @return GetCommentRepliesResponse containing replies and total count
     * @throws CommentNotFoundException if the parent comment does not exist
     */
    public GetCommentRepliesResponse getCommentReplies(GetCommentRepliesRequest request) {
        try {
            log.debug("Listing replies for comment ID: {} on page: {}, limit: {}", request.getCommentId(),
                    request.getPage(), request.getLimit());
            GetCommentRepliesResponse response = commentBlockingStub.getCommentReplies(request);
            log.debug("Replies listed successfully, total count: {}", response.getTotalCount());
            return response;
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
                throw new CommentNotFoundException("Parent comment with id " + request.getCommentId() + " not found");
            } else {
                // For UNAVAILABLE, INTERNAL, or other unexpected errors
                throw new RuntimeException("gRPC call to feedback-service failed while listing replies", e);
            }
        }
    }

    /**
     * Updates an existing comment.
     *
     * @param request Contains the ID of the comment to update and new text
     * @return Updated Comment object with all details
     * @throws CommentNotFoundException if the comment does not exist
     */
    public Comment updateComment(UpdateCommentRequest request) {
        try {
            log.debug("Updating comment with ID: {}", request.getId());
            Comment response = commentBlockingStub.updateComment(request);
            log.debug("Comment updated successfully: {}", response);
            return response;
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
                throw new CommentNotFoundException("Comment with id " + request.getId() + " not found");
            } else if (e.getStatus().getCode() == Status.Code.INVALID_ARGUMENT) {
                throw new IllegalArgumentException(e.getStatus().getDescription());
            } else {
                // For UNAVAILABLE, INTERNAL, or other unexpected errors
                throw new RuntimeException("gRPC call to feedback-service failed while updating comment", e);
            }
        }
    }

    /**
     * Deletes a comment by its ID.
     *
     * @param request Contains the ID of the comment to delete
     * @return true if deletion was successful, false otherwise
     * @throws CommentNotFoundException if the comment does not exist
     */
    public boolean deleteComment(DeleteCommentRequest request) {
        try {
            log.debug("Deleting comment with ID: {}", request.getId());
            return commentBlockingStub.deleteComment(request).getSuccess();
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
                throw new CommentNotFoundException("Comment with id " + request.getId() + " not found");
            } else {
                // For UNAVAILABLE, INTERNAL, or other unexpected errors
                log.error("Failed to delete comment with ID: {} due to gRPC error: {}", request.getId(), e.getStatus().getDescription());
                throw new RuntimeException("gRPC call to feedback-service failed while deleting comment", e);
            }
        } catch (NullPointerException e) {
            log.error("Null pointer exception occurred while deleting comment with ID: {}", request.getId(), e);
            throw new RuntimeException("Failed to delete comment due to null pointer exception", e);
        }
    }

} 