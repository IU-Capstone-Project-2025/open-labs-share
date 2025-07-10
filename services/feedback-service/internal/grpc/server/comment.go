package server

import (
	"context"
	"fmt"
	"log"

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
	log.Printf("gRPC CreateComment received: ContentId=%d, UserId=%d, ParentId=%v, Content=%q", req.ContentId, req.UserId, req.ParentId, req.Content)

	if req.ContentId <= 0 {
		log.Printf("gRPC CreateComment error: content_id is required")
		return nil, status.Error(codes.InvalidArgument, "content_id is required")
	}
	if req.UserId <= 0 {
		log.Printf("gRPC CreateComment error: user_id is required")
		return nil, status.Error(codes.InvalidArgument, "user_id is required")
	}
	if req.Content == "" {
		log.Printf("gRPC CreateComment error: content is required")
		return nil, status.Error(codes.InvalidArgument, "content is required")
	}

	var parentID *string
	if req.ParentId != nil {
		parentID = req.ParentId
	}

	comment, err := s.commentService.CreateComment(ctx, req.ContentId, req.UserId, parentID, req.Content)
	if err != nil {
		log.Printf("gRPC CreateComment error: failed to create comment: %v", err)
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
	}

	log.Printf("gRPC CreateComment response: Id=%s, ContentId=%d, UserId=%d, ParentId=%v", response.Id, response.ContentId, response.UserId, response.ParentId)
	return response, nil
}

// GetComment retrieves a comment by ID
func (s *commentServer) GetComment(ctx context.Context, req *pb.GetCommentRequest) (*pb.Comment, error) {
	log.Printf("gRPC GetComment received: Id=%s", req.Id)

	if req.Id == "" {
		log.Printf("gRPC GetComment error: comment ID is required")
		return nil, status.Error(codes.InvalidArgument, "comment ID is required")
	}

	comment, err := s.commentService.GetComment(ctx, req.Id)
	if err != nil {
		log.Printf("gRPC GetComment error: failed to get comment: %v", err)
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
	}

	log.Printf("gRPC GetComment response: Id=%s, ContentId=%d, UserId=%d, ParentId=%v", response.Id, response.ContentId, response.UserId, response.ParentId)
	return response, nil
}

// UpdateComment updates a comment
func (s *commentServer) UpdateComment(ctx context.Context, req *pb.UpdateCommentRequest) (*pb.Comment, error) {
	log.Printf("gRPC UpdateComment received: Id=%s, UserId=%d, Content=%q", req.Id, req.UserId, req.Content)

	// Validate request
	if req.UserId <= 0 {
		log.Printf("gRPC UpdateComment error: user_id is required")
		return nil, status.Error(codes.InvalidArgument, "user_id is required")
	}
	if req.Id == "" {
		log.Printf("gRPC UpdateComment error: comment ID is required")
		return nil, status.Error(codes.InvalidArgument, "comment ID is required")
	}
	if req.Content == "" {
		log.Printf("gRPC UpdateComment error: content is required")
		return nil, status.Error(codes.InvalidArgument, "content is required")
	}

	// Check if comment exists and user is authorized
	existingComment, err := s.commentService.GetComment(ctx, req.Id)
	if err != nil {
		log.Printf("gRPC UpdateComment error: comment not found: %v", err)
		return nil, status.Error(codes.NotFound, "comment not found")
	}

	// Authorization check: only the comment author can update it
	if existingComment.UserID != req.UserId {
		log.Printf("gRPC UpdateComment error: unauthorized - user %d cannot update comment owned by user %d", req.UserId, existingComment.UserID)
		return nil, status.Error(codes.PermissionDenied, "you can only update your own comments")
	}

	comment, err := s.commentService.UpdateComment(ctx, req.Id, req.Content)
	if err != nil {
		log.Printf("gRPC UpdateComment error: failed to update comment: %v", err)
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
	}

	log.Printf("gRPC UpdateComment response: Id=%s, ContentId=%d, UserId=%d, UpdatedAt=%v", response.Id, response.ContentId, response.UserId, response.UpdatedAt.AsTime())
	return response, nil
}

// DeleteComment deletes a comment
func (s *commentServer) DeleteComment(ctx context.Context, req *pb.DeleteCommentRequest) (*pb.DeleteCommentResponse, error) {
	log.Printf("gRPC DeleteComment received: Id=%s, UserId=%d", req.Id, req.UserId)

	// Validate request
	if req.UserId <= 0 {
		log.Printf("gRPC DeleteComment error: user_id is required")
		return nil, status.Error(codes.InvalidArgument, "user_id is required")
	}
	if req.Id == "" {
		log.Printf("gRPC DeleteComment error: comment ID is required")
		return nil, status.Error(codes.InvalidArgument, "comment ID is required")
	}

	// Check if comment exists and user is authorized
	existingComment, err := s.commentService.GetComment(ctx, req.Id)
	if err != nil {
		log.Printf("gRPC DeleteComment error: comment not found: %v", err)
		return nil, status.Error(codes.NotFound, "comment not found")
	}

	// Authorization check: only the comment author can delete it
	if existingComment.UserID != req.UserId {
		log.Printf("gRPC DeleteComment error: unauthorized - user %d cannot delete comment owned by user %d", req.UserId, existingComment.UserID)
		return nil, status.Error(codes.PermissionDenied, "you can only delete your own comments")
	}

	if err := s.commentService.DeleteComment(ctx, req.Id); err != nil {
		log.Printf("gRPC DeleteComment error: failed to delete comment: %v", err)
		return nil, status.Error(codes.Internal, fmt.Sprintf("failed to delete comment: %v", err))
	}

	response := &pb.DeleteCommentResponse{Success: true}
	log.Printf("gRPC DeleteComment response: Success=%t", response.Success)
	return response, nil
}

// ListComments lists comments by context
func (s *commentServer) ListComments(ctx context.Context, req *pb.ListCommentsRequest) (*pb.ListCommentsResponse, error) {
	log.Printf("gRPC ListComments received: ContentId=%d, ParentId=%v, Page=%d, Limit=%d", req.ContentId, req.ParentId, req.Page, req.Limit)

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

	log.Printf("gRPC ListComments response: %d comments, totalCount=%d", len(comments), totalCount)
	return &pb.ListCommentsResponse{
		Comments:   pbComments,
		TotalCount: totalCount,
	}, nil
}

// GetCommentReplies gets replies to a comment
func (s *commentServer) GetCommentReplies(ctx context.Context, req *pb.GetCommentRepliesRequest) (*pb.GetCommentRepliesResponse, error) {
	log.Printf("gRPC GetCommentReplies received: CommentId=%s, Page=%d, Limit=%d", req.CommentId, req.Page, req.Limit)

	if req.CommentId == "" {
		log.Printf("gRPC GetCommentReplies error: comment_id is required")
		return nil, status.Error(codes.InvalidArgument, "comment_id is required")
	}

	comments, totalCount, err := s.commentService.GetCommentReplies(ctx, req.CommentId, req.Page, req.Limit)
	if err != nil {
		log.Printf("gRPC GetCommentReplies error: failed to get comment replies: %v", err)
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

	response := &pb.GetCommentRepliesResponse{
		Comments:   pbComments,
		TotalCount: totalCount,
	}

	log.Printf("gRPC GetCommentReplies response: %d replies, totalCount=%d", len(comments), totalCount)
	return response, nil
}
