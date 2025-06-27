package olsh.backend.api_gateway.grpc.client;

import com.google.protobuf.Timestamp;
import io.grpc.Channel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import olsh.backend.api_gateway.dto.request.CreateCommentRequest;
import olsh.backend.api_gateway.dto.request.GetCommentsRequest;
import olsh.backend.api_gateway.dto.request.UpdateCommentRequest;
import olsh.backend.api_gateway.dto.response.CommentListResponse;
import olsh.backend.api_gateway.dto.response.CommentResponse;
import olsh.backend.api_gateway.exception.CommentNotFoundException;
import olsh.backend.api_gateway.grpc.proto.FeedbackServiceGrpc;
import olsh.backend.api_gateway.grpc.proto.FeedbackProto;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
public class CommentServiceClient {

    private final FeedbackServiceGrpc.FeedbackServiceBlockingStub feedbackServiceStub;

    public CommentServiceClient(GrpcChannelFactory channelFactory) {
        Channel channel = channelFactory.createChannel("feedback-service");
        this.feedbackServiceStub = FeedbackServiceGrpc.newBlockingStub(channel);
    }

    public CommentResponse createComment(long labId, long userId, CreateCommentRequest request) {
        FeedbackProto.CreateCommentRequest.Builder grpcRequestBuilder = FeedbackProto.CreateCommentRequest.newBuilder()
                .setLabId(labId)
                .setUserId(userId)
                .setContent(request.getContent());

        Optional.ofNullable(request.getParentId()).ifPresent(grpcRequestBuilder::setParentId);
        FeedbackProto.LabComment grpcResponse;
        try {
            grpcResponse = feedbackServiceStub.createComment(grpcRequestBuilder.build());
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

        return mapLabCommentToResponse(grpcResponse);
    }

    public CommentResponse getCommentById(String commentId) {
        FeedbackProto.GetCommentRequest request = FeedbackProto.GetCommentRequest.newBuilder()
                .setId(commentId)
                .build();
        
        FeedbackProto.LabComment grpcResponse;
        try {
            grpcResponse = feedbackServiceStub.getComment(request);
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
                throw new CommentNotFoundException("Comment with id " + commentId + " not found");
            } else {
                // For UNAVAILABLE, INTERNAL, or other unexpected errors
                throw new RuntimeException("gRPC call to feedback-service failed while getting comment", e);
            }
        }

        return mapLabCommentToResponse(grpcResponse);
    }

    public CommentListResponse getLabComments(long labId, GetCommentsRequest request) {
        FeedbackProto.ListLabCommentsRequest grpcRequest = FeedbackProto.ListLabCommentsRequest.newBuilder()
                .setLabId(labId)
                .setPage(request.getPage())
                .setLimit(request.getLimit())
                .build();

        FeedbackProto.ListLabCommentsResponse grpcResponse;
        try {
            grpcResponse = feedbackServiceStub.listLabComments(grpcRequest);
        } catch (StatusRuntimeException e) {
            // For UNAVAILABLE, INTERNAL, or other unexpected errors
            throw new RuntimeException("gRPC call to feedback-service failed while listing lab comments", e);
        }

        return CommentListResponse.builder()
                .comments(grpcResponse.getCommentsList().stream()
                        .map(this::mapLabCommentToResponse)
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
        FeedbackProto.GetCommentRepliesRequest grpcRequest = FeedbackProto.GetCommentRepliesRequest.newBuilder()
                .setCommentId(commentId)
                .setPage(request.getPage())
                .setLimit(request.getLimit())
                .build();

        FeedbackProto.GetCommentRepliesResponse grpcResponse;
        try {
            grpcResponse = feedbackServiceStub.getCommentReplies(grpcRequest);
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
                        .map(this::mapLabCommentToResponse)
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
        FeedbackProto.UpdateCommentRequest grpcRequest = FeedbackProto.UpdateCommentRequest.newBuilder()
                .setId(commentId)
                .setContent(request.getContent())
                .build();

        FeedbackProto.LabComment grpcResponse;
        try {
            grpcResponse = feedbackServiceStub.updateComment(grpcRequest);
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
        return mapLabCommentToResponse(grpcResponse);
    }

    public void deleteComment(String commentId) {
        FeedbackProto.DeleteCommentRequest grpcRequest = FeedbackProto.DeleteCommentRequest.newBuilder()
                .setId(commentId)
                .build();

        try {
            feedbackServiceStub.deleteComment(grpcRequest);
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
                throw new CommentNotFoundException("Comment with id " + commentId + " not found");
            } else {
                // For UNAVAILABLE, INTERNAL, or other unexpected errors
                throw new RuntimeException("gRPC call to feedback-service failed while deleting comment", e);
            }
        }
    }

    private CommentResponse mapLabCommentToResponse(FeedbackProto.LabComment labComment) {
        return CommentResponse.builder()
                .id(labComment.getId())
                .labId(labComment.getLabId())
                .userId(labComment.getUserId())
                .parentId(labComment.hasParentId() ? labComment.getParentId() : null)
                .content(labComment.getContent())
                .createdAt(formatTimestamp(labComment.getCreatedAt()))
                .updatedAt(formatTimestamp(labComment.getUpdatedAt()))
                .build();
    }

    private String formatTimestamp(Timestamp timestamp) {
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos())
                .atOffset(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_INSTANT);
    }
} 