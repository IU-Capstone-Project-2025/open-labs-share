package olsh.backend.api_gateway.grpc.client;

import com.google.protobuf.Timestamp;
import io.grpc.Channel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import olsh.backend.api_gateway.exception.CommentNotFoundException;
import olsh.backend.api_gateway.grpc.proto.CommentServiceGrpc;
import olsh.backend.api_gateway.grpc.proto.CommentProto;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.stereotype.Service;

import olsh.backend.api_gateway.dto.request.GetCommentsRequest;
import olsh.backend.api_gateway.dto.request.UpdateCommentRequest;
import olsh.backend.api_gateway.dto.request.CreateCommentRequest;
import olsh.backend.api_gateway.dto.response.CommentListResponse;
import olsh.backend.api_gateway.dto.response.CommentResponse;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
public class CommentServiceClient {

    private final CommentServiceGrpc.CommentServiceBlockingStub commentBlockingStub;

    public CommentServiceClient(GrpcChannelFactory channelFactory) {
        Channel channel = channelFactory.createChannel("feedback-service");
        this.commentBlockingStub = CommentServiceGrpc.newBlockingStub(channel);
    }

    public CommentResponse createComment(long labId, long userId, CreateCommentRequest request) {
        CommentProto.CreateCommentRequest.Builder grpcRequestBuilder = CommentProto.CreateCommentRequest.newBuilder()
                .setContentId(labId)
                .setUserId(userId)
                .setContent(request.getContent());

        Optional.ofNullable(request.getParentId()).ifPresent(grpcRequestBuilder::setParentId);
        CommentProto.Comment grpcResponse;
        try {
            grpcResponse = commentBlockingStub.createComment(grpcRequestBuilder.build());
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.INVALID_ARGUMENT) {
                // Translate to a 400 Bad Request. You might create a custom exception for this.
                // For now, re-throwing a generic exception is an option.
                throw new IllegalArgumentException(e.getStatus().getDescription());
            } else {
                // For UNAVAILABLE, INTERNAL, or other unexpected errors, throw a generic 500-level exception.
                throw new RuntimeException("gRPC call to feedback-service failed", e);
            }
        }

        return mapCommentToResponse(grpcResponse);
    }

    public CommentResponse getCommentById(String commentId) {
        CommentProto.GetCommentRequest request = CommentProto.GetCommentRequest.newBuilder()
                .setId(commentId)
                .build();
        
        CommentProto.Comment grpcResponse;
        try {
            grpcResponse = commentBlockingStub.getComment(request);
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
                throw new CommentNotFoundException("Comment with id " + commentId + " not found");
            } else {
                // For UNAVAILABLE, INTERNAL, or other unexpected errors
                throw new RuntimeException("gRPC call to feedback-service failed while getting comment", e);
            }
        }

        return mapCommentToResponse(grpcResponse);
    }

    public CommentListResponse getComments(long labId, GetCommentsRequest request) {
        CommentProto.ListCommentsRequest grpcRequest = CommentProto.ListCommentsRequest.newBuilder()
                .setContentId(labId)
                .setPage(request.getPage())
                .setLimit(request.getLimit())
                .build();

        CommentProto.ListCommentsResponse grpcResponse;
        try {
            grpcResponse = commentBlockingStub.listComments(grpcRequest);
        } catch (StatusRuntimeException e) {
            // For UNAVAILABLE, INTERNAL, or other unexpected errors
            throw new RuntimeException("gRPC call to feedback-service failed while listing lab comments", e);
        }

        return CommentListResponse.builder()
                .comments(grpcResponse.getCommentsList().stream()
                        .map(this::mapCommentToResponse)
                        .toList())
                .pagination(
                        CommentListResponse.PaginationResponse.builder()
                                .currentPage(grpcResponse.getTotalCount() > 0 ? request.getPage() : 0)
                                .totalItems(grpcResponse.getTotalCount())
                                .totalPages((int) Math.ceil((double) grpcResponse.getTotalCount() / request.getLimit()))
                                .build()
                )
                .build();
    }

    public CommentListResponse getCommentReplies(String commentId, GetCommentsRequest request) {
        CommentProto.GetCommentRepliesRequest grpcRequest = CommentProto.GetCommentRepliesRequest.newBuilder()
                .setCommentId(commentId)
                .setPage(request.getPage())
                .setLimit(request.getLimit())
                .build();

        CommentProto.GetCommentRepliesResponse grpcResponse;
        try {
            grpcResponse = commentBlockingStub.getCommentReplies(grpcRequest);
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
                throw new CommentNotFoundException("Parent comment with id " + commentId + " not found");
            } else {
                // For UNAVAILABLE, INTERNAL, or other unexpected errors
                throw new RuntimeException("gRPC call to feedback-service failed while listing replies", e);
            }
        }

        return CommentListResponse.builder()
                .comments(grpcResponse.getCommentsList().stream()
                        .map(this::mapCommentToResponse)
                        .toList())
                .pagination(
                        CommentListResponse.PaginationResponse.builder()
                                .currentPage(grpcResponse.getTotalCount() > 0 ? request.getPage() : 0)
                                .totalItems(grpcResponse.getTotalCount())
                                .totalPages((int) Math.ceil((double) grpcResponse.getTotalCount() / request.getLimit()))
                                .build()
                )
                .build();
    }

    public CommentResponse updateComment(String commentId, UpdateCommentRequest request) {
        CommentProto.UpdateCommentRequest grpcRequest = CommentProto.UpdateCommentRequest.newBuilder()
                .setId(commentId)
                .setContent(request.getContent())
                .build();

        CommentProto.Comment grpcResponse;
        try {
            grpcResponse = commentBlockingStub.updateComment(grpcRequest);
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
                throw new CommentNotFoundException("Comment with id " + commentId + " not found");
            } else if (e.getStatus().getCode() == Status.Code.INVALID_ARGUMENT) {
                throw new IllegalArgumentException(e.getStatus().getDescription());
            } else {
                // For UNAVAILABLE, INTERNAL, or other unexpected errors
                throw new RuntimeException("gRPC call to feedback-service failed while updating comment", e);
            }
        }
        return mapCommentToResponse(grpcResponse);
    }

    public void deleteComment(String commentId) {
        CommentProto.DeleteCommentRequest grpcRequest = CommentProto.DeleteCommentRequest.newBuilder()
                .setId(commentId)
                .build();

        try {
            commentBlockingStub.deleteComment(grpcRequest);
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
                throw new CommentNotFoundException("Comment with id " + commentId + " not found");
            } else {
                // For UNAVAILABLE, INTERNAL, or other unexpected errors
                throw new RuntimeException("gRPC call to feedback-service failed while deleting comment", e);
            }
        }
    }

    private CommentResponse mapCommentToResponse(CommentProto.Comment Comment) {
        return CommentResponse.builder()
                .id(Comment.getId())
                .labId(Comment.getContentId())
                .userId(Comment.getUserId())
                .parentId(Comment.hasParentId() ? Comment.getParentId() : null)
                .content(Comment.getContent())
                .createdAt(formatTimestamp(Comment.getCreatedAt()))
                .updatedAt(formatTimestamp(Comment.getUpdatedAt()))
                .build();
    }

    private String formatTimestamp(Timestamp timestamp) {
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos())
                .atOffset(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_INSTANT);
    }
} 