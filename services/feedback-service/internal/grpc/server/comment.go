package server

import (
	"context"
	"fmt"

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
}

// NewCommentServer creates a new comment server
func NewCommentServer(commentService *service.CommentService) pb.CommentServiceServer {
	return &commentServer{
		commentService: commentService,
	}
}

// RegisterCommentServer registers the comment server with gRPC
func RegisterCommentServer(s *grpc.Server, commentService *service.CommentService) {
	server := &commentServer{
		commentService: commentService,
	}
	pb.RegisterCommentServiceServer(s, server)
}

// CreateComment creates a new comment
func (s *commentServer) CreateComment(ctx context.Context, req *pb.CreateCommentRequest) (*pb.Comment, error) {
	if req.ContentId <= 0 {
		return nil, status.Error(codes.InvalidArgument, "content_id is required")
	}
	if req.UserId <= 0 {
		return nil, status.Error(codes.InvalidArgument, "user_id is required")
	}
	if req.Content == "" {
		return nil, status.Error(codes.InvalidArgument, "content is required")
	}

	var parentID *string
	if req.ParentId != nil {
		parentID = req.ParentId
	}

	comment, err := s.commentService.CreateComment(ctx, req.ContentId, req.UserId, parentID, req.Content)
	if err != nil {
		return nil, status.Error(codes.Internal, fmt.Sprintf("failed to create comment: %v", err))
	}

	return &pb.Comment{
		Id:        comment.ID.Hex(),
		ContentId: comment.ContentID,
		UserId:    comment.UserID,
		ParentId:  comment.ParentID,
		Content:   comment.Content,
		CreatedAt: timestamppb.New(comment.CreatedAt),
		UpdatedAt: timestamppb.New(comment.UpdatedAt),
	}, nil
}

// GetComment retrieves a comment by ID
func (s *commentServer) GetComment(ctx context.Context, req *pb.GetCommentRequest) (*pb.Comment, error) {
	if req.Id == "" {
		return nil, status.Error(codes.InvalidArgument, "comment ID is required")
	}

	comment, err := s.commentService.GetComment(ctx, req.Id)
	if err != nil {
		return nil, status.Error(codes.Internal, fmt.Sprintf("failed to get comment: %v", err))
	}

	return &pb.Comment{
		Id:        comment.ID.Hex(),
		ContentId: comment.ContentID,
		UserId:    comment.UserID,
		ParentId:  comment.ParentID,
		Content:   comment.Content,
		CreatedAt: timestamppb.New(comment.CreatedAt),
		UpdatedAt: timestamppb.New(comment.UpdatedAt),
	}, nil
}

// UpdateComment updates a comment
func (s *commentServer) UpdateComment(ctx context.Context, req *pb.UpdateCommentRequest) (*pb.Comment, error) {
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
		return nil, status.Error(codes.NotFound, "comment not found")
	}

	// Authorization check: only the comment author can update it
	if existingComment.UserID != req.UserId {
		return nil, status.Error(codes.PermissionDenied, "you can only update your own comments")
	}

	comment, err := s.commentService.UpdateComment(ctx, req.Id, req.Content)
	if err != nil {
		return nil, status.Error(codes.Internal, fmt.Sprintf("failed to update comment: %v", err))
	}

	return &pb.Comment{
		Id:        comment.ID.Hex(),
		ContentId: comment.ContentID,
		UserId:    comment.UserID,
		ParentId:  comment.ParentID,
		Content:   comment.Content,
		CreatedAt: timestamppb.New(comment.CreatedAt),
		UpdatedAt: timestamppb.New(comment.UpdatedAt),
	}, nil
}

// DeleteComment deletes a comment
func (s *commentServer) DeleteComment(ctx context.Context, req *pb.DeleteCommentRequest) (*pb.DeleteCommentResponse, error) {
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
		return nil, status.Error(codes.NotFound, "comment not found")
	}

	// Authorization check: only the comment author can delete it
	if existingComment.UserID != req.UserId {
		return nil, status.Error(codes.PermissionDenied, "you can only delete your own comments")
	}

	if err := s.commentService.DeleteComment(ctx, req.Id); err != nil {
		return nil, status.Error(codes.Internal, fmt.Sprintf("failed to delete comment: %v", err))
	}

	return &pb.DeleteCommentResponse{Success: true}, nil
}

// ListComments lists comments by context
func (s *commentServer) ListComments(ctx context.Context, req *pb.ListCommentsRequest) (*pb.ListCommentsResponse, error) {
	if req.ContentId <= 0 {
		return nil, status.Error(codes.InvalidArgument, "content_id is required")
	}

	var parentID *string
	if req.ParentId != nil {
		parentID = req.ParentId
	}

	comments, totalCount, err := s.commentService.ListComments(ctx, req.ContentId, parentID, req.Page, req.Limit)
	if err != nil {
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
		}
	}

	return &pb.ListCommentsResponse{
		Comments:   pbComments,
		TotalCount: totalCount,
	}, nil
}

// GetCommentReplies gets replies to a comment
func (s *commentServer) GetCommentReplies(ctx context.Context, req *pb.GetCommentRepliesRequest) (*pb.GetCommentRepliesResponse, error) {
	if req.CommentId == "" {
		return nil, status.Error(codes.InvalidArgument, "comment_id is required")
	}

	comments, totalCount, err := s.commentService.GetCommentReplies(ctx, req.CommentId, req.Page, req.Limit)
	if err != nil {
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
		}
	}

	return &pb.GetCommentRepliesResponse{
		Comments:   pbComments,
		TotalCount: totalCount,
	}, nil
}
