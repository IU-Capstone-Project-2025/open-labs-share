// Trigger CI
package main

import (
	"context"
	"fmt"
	"log/slog"
	"net"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/IU-Capstone-Project-2025/open-labs-share/services/feedback-service/internal/config"
	"github.com/IU-Capstone-Project-2025/open-labs-share/services/feedback-service/internal/database"
	"github.com/IU-Capstone-Project-2025/open-labs-share/services/feedback-service/internal/grpc/server"
	"github.com/IU-Capstone-Project-2025/open-labs-share/services/feedback-service/internal/middleware"
	"github.com/IU-Capstone-Project-2025/open-labs-share/services/feedback-service/internal/repository"
	"github.com/IU-Capstone-Project-2025/open-labs-share/services/feedback-service/internal/service"
	"github.com/minio/minio-go/v7"
	"github.com/minio/minio-go/v7/pkg/credentials"
	"google.golang.org/grpc"
	"google.golang.org/grpc/health"
	healthpb "google.golang.org/grpc/health/grpc_health_v1"
	"google.golang.org/grpc/keepalive"
	"google.golang.org/grpc/reflection"
)

func main() {
	// Initialize structured logger
	logger := slog.New(slog.NewJSONHandler(os.Stdout, nil))
	slog.SetDefault(logger)

	// Load configuration
	cfg, err := config.Load()
	if err != nil {
		logger.Error("Failed to load configuration", "error", err)
		os.Exit(1)
	}

	// Initialize PostgreSQL database connection
	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	db, err := database.NewConnection(ctx, cfg.Database)
	if err != nil {
		logger.Error("Failed to connect to PostgreSQL", "error", err)
		os.Exit(1)
	}
	defer db.Close()

	// Run database migrations
	if err := database.Migrate(ctx, db, "migrations"); err != nil {
		logger.Error("Failed to run database migrations", "error", err)
		os.Exit(1)
	}

	// Initialize MongoDB connection
	mongodb, err := database.ConnectMongoDB(ctx, cfg.MongoDB)
	if err != nil {
		logger.Error("Failed to connect to MongoDB", "error", err)
		os.Exit(1)
	}
	defer mongodb.Close(context.Background())

	// Create MongoDB indexes
	if err := mongodb.CreateIndexes(ctx, cfg.MongoDB.Collection); err != nil {
		logger.Error("Failed to create MongoDB indexes", "error", err)
		os.Exit(1)
	}

	// Initialize MinIO client
	minioClient, err := minio.New(cfg.MinIO.Endpoint, &minio.Options{
		Creds:  credentials.NewStaticV4(cfg.MinIO.AccessKey, cfg.MinIO.SecretKey, ""),
		Secure: cfg.MinIO.UseSSL,
	})
	if err != nil {
		logger.Error("Failed to initialize MinIO client", "error", err)
		os.Exit(1)
	}

	// Create bucket if it doesn't exist
	if cfg.MinIO.CreateBucket {
		exists, err := minioClient.BucketExists(ctx, cfg.MinIO.BucketName)
		if err != nil {
			logger.Error("Failed to check if bucket exists", "error", err)
			os.Exit(1)
		}
		if !exists {
			err = minioClient.MakeBucket(ctx, cfg.MinIO.BucketName, minio.MakeBucketOptions{})
			if err != nil {
				logger.Error("Failed to create bucket", "error", err)
				os.Exit(1)
			}
			logger.Info("Created bucket", "bucket", cfg.MinIO.BucketName)
		}
	}

	// Set bucket policy for public read access
	policy := fmt.Sprintf(`{
		"Version": "2012-10-17",
		"Statement": [
			{
				"Effect": "Allow",
				"Principal": "*",
				"Action": ["s3:GetObject"],
				"Resource": ["arn:aws:s3:::%s/*"]
			}
		]
	}`, cfg.MinIO.BucketName)
	err = minioClient.SetBucketPolicy(ctx, cfg.MinIO.BucketName, policy)
	if err != nil {
		logger.Error("Failed to set bucket policy", "error", err)
		os.Exit(1)
	}
	logger.Info("Set read-only policy for bucket", "bucket", cfg.MinIO.BucketName)

	// Initialize repositories
	feedbackRepo := repository.NewFeedbackRepository(db, mongodb)
	attachmentRepo := repository.NewAttachmentRepository(minioClient, cfg.MinIO.BucketName, cfg.MinIO.Endpoint, cfg.MinIO.UseSSL)
	commentRepo := repository.NewCommentRepository(mongodb, cfg.MongoDB.Collection)

	// Initialize services
	feedbackService := service.NewFeedbackService(feedbackRepo, attachmentRepo, logger)
	commentService := service.NewCommentService(commentRepo, logger)

	// Create gRPC server with improved streaming error handling
	grpcServer := grpc.NewServer(
		grpc.MaxRecvMsgSize(32*1024*1024), // 32MB max message size for file uploads
		grpc.MaxSendMsgSize(32*1024*1024), // 32MB max message size for file downloads
		grpc.StreamInterceptor(middleware.StreamingServerInterceptor()),
		grpc.ConnectionTimeout(30*time.Second), // Connection timeout
		grpc.KeepaliveParams(keepalive.ServerParameters{
			MaxConnectionIdle:     30 * time.Second, // Connection idle timeout
			MaxConnectionAge:      5 * time.Minute,  // Max connection age
			MaxConnectionAgeGrace: 5 * time.Second,  // Grace period for connection age
			Time:                  5 * time.Second,  // Keepalive ping interval
			Timeout:               1 * time.Second,  // Keepalive ping timeout
		}),
		grpc.KeepaliveEnforcementPolicy(keepalive.EnforcementPolicy{
			MinTime:             5 * time.Second, // Min time between keepalive pings
			PermitWithoutStream: true,            // Allow keepalive without active streams
		}),
	)

	// Register services
	server.RegisterFeedbackServer(grpcServer, feedbackService, logger)
	server.RegisterCommentServer(grpcServer, commentService, logger)

	// Create a new health server and register it
	healthServer := health.NewServer()
	healthpb.RegisterHealthServer(grpcServer, healthServer)
	healthServer.SetServingStatus("feedback.FeedbackService", healthpb.HealthCheckResponse_SERVING)
	healthServer.SetServingStatus("comment.CommentService", healthpb.HealthCheckResponse_SERVING)

	// Enable reflection for easier debugging
	reflection.Register(grpcServer)

	// Start server
	listener, err := net.Listen("tcp", ":"+cfg.GRPCPort)
	if err != nil {
		logger.Error("Failed to listen on port", "port", cfg.GRPCPort, "error", err)
		os.Exit(1)
	}

	logger.Info("Starting gRPC server", "port", cfg.GRPCPort)

	// Graceful shutdown
	go func() {
		if err := grpcServer.Serve(listener); err != nil {
			logger.Error("Failed to serve gRPC server", "error", err)
			os.Exit(1)
		}
	}()

	// Wait for interrupt signal to gracefully shutdown the server
	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)
	<-quit

	logger.Info("Shutting down server...")

	// Graceful shutdown with timeout
	shutdownCtx, shutdownCancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer shutdownCancel()

	done := make(chan bool, 1)
	go func() {
		grpcServer.GracefulStop()
		done <- true
	}()

	select {
	case <-done:
		logger.Info("Server stopped gracefully")
	case <-shutdownCtx.Done():
		logger.Warn("Server shutdown timeout, forcing stop")
		grpcServer.Stop()
	}
}
