package middleware

import (
	"context"
	"fmt"
	"io"
	"log"

	"google.golang.org/grpc"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
)

// StreamingServerInterceptor provides better error handling for streaming operations
func StreamingServerInterceptor() grpc.StreamServerInterceptor {
	return func(srv interface{}, ss grpc.ServerStream, info *grpc.StreamServerInfo, handler grpc.StreamHandler) error {
		// Wrap the server stream to add error handling
		wrappedStream := &wrappedServerStream{
			ServerStream: ss,
			ctx:          ss.Context(),
		}

		// Call the handler
		err := handler(srv, wrappedStream)
		
		// Enhanced error handling for streaming operations
		if err != nil {
			// Log the error for debugging
			log.Printf("Streaming RPC error in %s: %v", info.FullMethod, err)
			
			// Check for common streaming errors and convert them to appropriate gRPC status
			if isStreamingError(err) {
				return handleStreamingError(err)
			}
			
			// Return the error as-is if it's already a gRPC status error
			if _, ok := status.FromError(err); ok {
				return err
			}
			
			// Convert unknown errors to internal server error
			return status.Error(codes.Internal, fmt.Sprintf("streaming operation failed: %v", err))
		}
		
		return nil
	}
}

// wrappedServerStream wraps the grpc.ServerStream to provide additional functionality
type wrappedServerStream struct {
	grpc.ServerStream
	ctx context.Context
}

func (w *wrappedServerStream) Context() context.Context {
	return w.ctx
}

// isStreamingError checks if the error is related to streaming operations
func isStreamingError(err error) bool {
	if err == nil {
		return false
	}
	
	errorStr := err.Error()
	
	// Common streaming-related errors
	streamingErrors := []string{
		"io: read/write on closed pipe",
		"broken pipe",
		"connection reset by peer",
		"context canceled",
		"context deadline exceeded",
		"stream terminated by client",
		"transport is closing",
		"EOF",
	}
	
	for _, streamErr := range streamingErrors {
		if contains(errorStr, streamErr) {
			return true
		}
	}
	
	// Check for specific error types
	if err == io.EOF {
		return true
	}
	
	if err == context.Canceled {
		return true
	}
	
	if err == context.DeadlineExceeded {
		return true
	}
	
	return false
}

// handleStreamingError converts streaming errors to appropriate gRPC status codes
func handleStreamingError(err error) error {
	if err == nil {
		return nil
	}
	
	errorStr := err.Error()
	
	// Context-related errors
	if err == context.Canceled || contains(errorStr, "context canceled") {
		return status.Error(codes.Canceled, "request was cancelled")
	}
	
	if err == context.DeadlineExceeded || contains(errorStr, "context deadline exceeded") {
		return status.Error(codes.DeadlineExceeded, "request deadline exceeded")
	}
	
	// Connection-related errors
	if contains(errorStr, "io: read/write on closed pipe") || 
	   contains(errorStr, "broken pipe") {
		return status.Error(codes.Unavailable, "connection closed during streaming operation")
	}
	
	if contains(errorStr, "connection reset by peer") {
		return status.Error(codes.Unavailable, "connection was reset by client")
	}
	
	if contains(errorStr, "transport is closing") {
		return status.Error(codes.Unavailable, "transport connection is closing")
	}
	
	// EOF during streaming (normal termination)
	if err == io.EOF || contains(errorStr, "EOF") {
		return status.Error(codes.OutOfRange, "stream ended unexpectedly")
	}
	
	// Default to internal error for unhandled streaming errors
	return status.Error(codes.Internal, fmt.Sprintf("streaming error: %v", err))
}

// contains checks if a string contains a substring (case-insensitive helper)
func contains(s, substr string) bool {
	return len(s) >= len(substr) && 
		   (s == substr || 
			len(s) > len(substr) && 
			(s[:len(substr)] == substr || 
			 s[len(s)-len(substr):] == substr || 
			 findSubstring(s, substr)))
}

// findSubstring is a simple substring search
func findSubstring(s, substr string) bool {
	for i := 0; i <= len(s)-len(substr); i++ {
		if s[i:i+len(substr)] == substr {
			return true
		}
	}
	return false
}
