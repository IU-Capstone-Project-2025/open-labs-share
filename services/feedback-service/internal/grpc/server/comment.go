package server

import (
	"context"
	"fmt"
	"log/slog"

	pb "github.com/IU-Capstone-Project-2025/open-labs-share/services/feedback-service/api"
	"github.com/IU-Capstone-Project-2025/open-labs-share/services/feedback-service/internal/service"
	"google.golang.org/grpc"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
	"google.golang.org/protobuf/types/known/timestamppb"
)

// commentServer implements the CommentService gRPC server
type commentServer struct {
	pb.UnimplementedCommentServiceServer
	commentService *service.CommentService
	logger         *slog.Logger
}

// NewCommentServer creates a new comment server
func NewCommentServer(commentService *service.CommentService, logger *slog.Logger) pb.CommentServiceServer {
	return &commentServer{
		commentService: commentService,
		logger:         logger,
	}
}

// RegisterCommentServer registers the comment server with gRPC
func RegisterCommentServer(s *grpc.Server, commentService *service.CommentService, logger *slog.Logger) {
	server := &commentServer{
		commentService: commentService,
		logger:         logger,
	}
	pb.RegisterCommentServiceServer(s, server)
}

// CreateComment creates a new comment
func (s *commentServer) CreateComment(ctx context.Context, req *pb.CreateCommentRequest) (*pb.Comment, error) {
	s.logger.Info("gRPC CreateComment received",
		"content_id", req.ContentId,
		"user_id", req.UserId,
		"parent_id", req.ParentId,
		"type", req.Type,
	)

	if req.ContentId <= 0 {
		return nil, status.Error(codes.InvalidArgument, "content_id is required")
	}
	if req.UserId <= 0 {
		return nil, status.Error(codes.InvalidArgument, "user_id is required")
	}
	if req.Content == "" {
		return nil, status.Error(codes.InvalidArgument, "content is required")
	}
	if req.Type == "" {
		return nil, status.Error(codes.InvalidArgument, "type is required")
	}
	if req.Type != "lab" && req.Type != "article" {
		return nil, status.Error(codes.InvalidArgument, "type must be 'lab' or 'article'")
	}

	var parentID *string
	if req.ParentId != nil {
		parentID = req.ParentId
	}

	comment, err := s.commentService.CreateComment(ctx, req.ContentId, req.UserId, parentID, req.Content, req.Type)
	if err != nil {
		s.logger.Error("gRPC CreateComment failed", "error", err)
		return nil, status.Error(codes.Internal, fmt.Sprintf("failed to create comment: %v", err))
	}

	response := &pb.Comment{
		Id:        comment.ID.Hex(),
		ContentId: comment.ContentID,
		UserId:    comment.UserID,
		ParentId:  comment.ParentID,
		Content:   comment.Content,
		CreatedAt: timestamppb.New(comment.CreatedAt),
		UpdatedAt: timestamppb.New(comment.UpdatedAt),
		Type:      comment.Type,
	}

	s.logger.Info("gRPC CreateComment completed", "comment_id", response.Id)
	return response, nil
}

// GetComment retrieves a comment by ID
func (s *commentServer) GetComment(ctx context.Context, req *pb.GetCommentRequest) (*pb.Comment, error) {
	s.logger.Info("gRPC GetComment received", "id", req.Id)

	if req.Id == "" {
		return nil, status.Error(codes.InvalidArgument, "comment ID is required")
	}

	comment, err := s.commentService.GetComment(ctx, req.Id)
	if err != nil {
		s.logger.Error("gRPC GetComment failed", "id", req.Id, "error", err)
		return nil, status.Error(codes.Internal, fmt.Sprintf("failed to get comment: %v", err))
	}

	response := &pb.Comment{
		Id:        comment.ID.Hex(),
		ContentId: comment.ContentID,
		UserId:    comment.UserID,
		ParentId:  comment.ParentID,
		Content:   comment.Content,
		CreatedAt: timestamppb.New(comment.CreatedAt),
		UpdatedAt: timestamppb.New(comment.UpdatedAt),
		Type:      comment.Type,
	}

	s.logger.Info("gRPC GetComment completed", "id", response.Id)
	return response, nil
}

// UpdateComment updates a comment
func (s *commentServer) UpdateComment(ctx context.Context, req *pb.UpdateCommentRequest) (*pb.Comment, error) {
	s.logger.Info("gRPC UpdateComment received",
		"id", req.Id,
		"user_id", req.UserId,
	)

	// Validate request
	if req.UserId <= 0 {
		return nil, status.Error(codes.InvalidArgument, "user_id is required")
	}
	if req.Id == "" {
		return nil, status.Error(codes.InvalidArgument, "comment ID is required")
	}
	if req.Content == "" {
		return nil, status.Error(codes.InvalidArgument, "content is required")
	}

	// Check if comment exists and user is authorized
	existingComment, err := s.commentService.GetComment(ctx, req.Id)
	if err != nil {
		s.logger.Warn("gRPC UpdateComment: comment not found", "id", req.Id, "error", err)
		return nil, status.Error(codes.NotFound, "comment not found")
	}

	// Authorization check: only the comment author can update it
	if existingComment.UserID != req.UserId {
		s.logger.Warn("gRPC UpdateComment: permission denied",
			"id", req.Id,
			"user_id", req.UserId,
			"owner_id", existingComment.UserID,
		)
		return nil, status.Error(codes.PermissionDenied, "you can only update your own comments")
	}

	comment, err := s.commentService.UpdateComment(ctx, req.Id, req.Content)
	if err != nil {
		s.logger.Error("gRPC UpdateComment failed", "id", req.Id, "error", err)
		return nil, status.Error(codes.Internal, fmt.Sprintf("failed to update comment: %v", err))
	}

	response := &pb.Comment{
		Id:        comment.ID.Hex(),
		ContentId: comment.ContentID,
		UserId:    comment.UserID,
		ParentId:  comment.ParentID,
		Content:   comment.Content,
		CreatedAt: timestamppb.New(comment.CreatedAt),
		UpdatedAt: timestamppb.New(comment.UpdatedAt),
		Type:      comment.Type,
	}

	s.logger.Info("gRPC UpdateComment completed", "id", response.Id)
	return response, nil
}

// DeleteComment deletes a comment
func (s *commentServer) DeleteComment(ctx context.Context, req *pb.DeleteCommentRequest) (*pb.DeleteCommentResponse, error) {
	s.logger.Info("gRPC DeleteComment received",
		"id", req.Id,
		"user_id", req.UserId,
	)

	// Validate request
	if req.UserId <= 0 {
		return nil, status.Error(codes.InvalidArgument, "user_id is required")
	}
	if req.Id == "" {
		return nil, status.Error(codes.InvalidArgument, "comment ID is required")
	}

	// Check if comment exists and user is authorized
	existingComment, err := s.commentService.GetComment(ctx, req.Id)
	if err != nil {
		s.logger.Warn("gRPC DeleteComment: comment not found", "id", req.Id, "error", err)
		return nil, status.Error(codes.NotFound, "comment not found")
	}

	// Authorization check: only the comment author can delete it
	if existingComment.UserID != req.UserId {
		s.logger.Warn("gRPC DeleteComment: permission denied",
			"id", req.Id,
			"user_id", req.UserId,
			"owner_id", existingComment.UserID,
		)
		return nil, status.Error(codes.PermissionDenied, "you can only delete your own comments")
	}

	if err := s.commentService.DeleteComment(ctx, req.Id); err != nil {
		s.logger.Error("gRPC DeleteComment failed", "id", req.Id, "error", err)
		return nil, status.Error(codes.Internal, fmt.Sprintf("failed to delete comment: %v", err))
	}

	response := &pb.DeleteCommentResponse{Success: true}
	s.logger.Info("gRPC DeleteComment completed", "id", req.Id)
	return response, nil
}

// ListComments lists comments by context
func (s *commentServer) ListComments(ctx context.Context, req *pb.ListCommentsRequest) (*pb.ListCommentsResponse, error) {
	s.logger.Info("gRPC ListComments received",
		"content_id", req.ContentId,
		"parent_id", req.ParentId,
		"page", req.Page,
		"limit", req.Limit,
		"type", req.Type,
	)

	if req.ContentId <= 0 {
		return nil, status.Error(codes.InvalidArgument, "content_id is required")
	}
	if req.Type == "" {
		return nil, status.Error(codes.InvalidArgument, "type is required")
	}

	var parentID *string
	if req.ParentId != nil {
		parentID = req.ParentId
	}

	comments, totalCount, err := s.commentService.ListComments(ctx, req.ContentId, parentID, req.Page, req.Limit, req.Type)
	if err != nil {
		s.logger.Error("gRPC ListComments failed", "error", err)
		return nil, status.Error(codes.Internal, fmt.Sprintf("failed to list comments: %v", err))
	}

	pbComments := make([]*pb.Comment, len(comments))
	for i, comment := range comments {
		pbComments[i] = &pb.Comment{
			Id:        comment.ID.Hex(),
			ContentId: comment.ContentID,
			UserId:    comment.UserID,
			ParentId:  comment.ParentID,
			Content:   comment.Content,
			CreatedAt: timestamppb.New(comment.CreatedAt),
			UpdatedAt: timestamppb.New(comment.UpdatedAt),
			Type:      comment.Type,
		}
	}

	s.logger.Info("gRPC ListComments completed",
		"count", len(comments),
		"total_count", totalCount,
	)
	return &pb.ListCommentsResponse{
		Comments:   pbComments,
		TotalCount: totalCount,
	}, nil
}

// GetCommentReplies gets replies to a comment
func (s *commentServer) GetCommentReplies(ctx context.Context, req *pb.GetCommentRepliesRequest) (*pb.GetCommentRepliesResponse, error) {
	s.logger.Info("gRPC GetCommentReplies received",
		"comment_id", req.CommentId,
		"page", req.Page,
		"limit", req.Limit,
	)

	if req.CommentId == "" {
		return nil, status.Error(codes.InvalidArgument, "comment_id is required")
	}

	comments, totalCount, err := s.commentService.GetCommentReplies(ctx, req.CommentId, req.Page, req.Limit)
	if err != nil {
		s.logger.Error("gRPC GetCommentReplies failed", "comment_id", req.CommentId, "error", err)
		return nil, status.Error(codes.Internal, fmt.Sprintf("failed to get comment replies: %v", err))
	}

	pbComments := make([]*pb.Comment, len(comments))
	for i, comment := range comments {
		pbComments[i] = &pb.Comment{
			Id:        comment.ID.Hex(),
			ContentId: comment.ContentID,
			UserId:    comment.UserID,
			ParentId:  comment.ParentID,
			Content:   comment.Content,
			CreatedAt: timestamppb.New(comment.CreatedAt),
			UpdatedAt: timestamppb.New(comment.UpdatedAt),
			Type:      comment.Type,
		}
	}

	response := &pb.GetCommentRepliesResponse{
		Comments:   pbComments,
		TotalCount: totalCount,
	}

	s.logger.Info("gRPC GetCommentReplies completed",
		"comment_id", req.CommentId,
		"count", len(comments),
		"total_count", totalCount,
	)
	return response, nil
}
