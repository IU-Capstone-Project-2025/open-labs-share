package middleware

import (
	"context"
	"fmt"
	"io"
	"log/slog"

	"google.golang.org/grpc"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
)

// StreamingServerInterceptor provides better error handling for streaming operations
func StreamingServerInterceptor() grpc.StreamServerInterceptor {
	return func(srv interface{}, ss grpc.ServerStream, info *grpc.StreamServerInfo, handler grpc.StreamHandler) error {
		wrappedStream := &wrappedServerStream{
			ServerStream: ss,
			ctx:          ss.Context(),
		}

		err := handler(srv, wrappedStream)
		if err != nil {
			slog.Error("Streaming RPC error", "method", info.FullMethod, "error", err)

			// Handle specific, non-gRPC errors first
			if err == io.EOF {
				// EOF is often a normal termination of a stream.
				// If it needs to be an error, the service handler should convert it.
				return nil
			}
			if err == context.Canceled {
				return status.Error(codes.Canceled, "request was cancelled by the client")
			}
			if err == context.DeadlineExceeded {
				return status.Error(codes.DeadlineExceeded, "request deadline exceeded")
			}

			// If it's already a gRPC status error, return it directly
			if _, ok := status.FromError(err); ok {
				return err
			}

			// For other errors, convert to a generic internal error
			return status.Error(codes.Internal, fmt.Sprintf("an unexpected error occurred: %v", err))
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
