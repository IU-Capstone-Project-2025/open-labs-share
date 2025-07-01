package main

import (
	"context"
	"log"
	"net"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/IU-Capstone-Project-2025/open-labs-share/services/feedback-service/internal/config"
	"github.com/IU-Capstone-Project-2025/open-labs-share/services/feedback-service/internal/database"
	"github.com/IU-Capstone-Project-2025/open-labs-share/services/feedback-service/internal/grpc/server"
	"github.com/IU-Capstone-Project-2025/open-labs-share/services/feedback-service/internal/repository"
	"github.com/IU-Capstone-Project-2025/open-labs-share/services/feedback-service/internal/service"
	"github.com/minio/minio-go/v7"
	"github.com/minio/minio-go/v7/pkg/credentials"
	"google.golang.org/grpc"
	"google.golang.org/grpc/reflection"
)

func main() {
	// Load configuration
	cfg, err := config.Load()
	if err != nil {
		log.Fatalf("Failed to load configuration: %v", err)
	}

	// Initialize PostgreSQL database connection
	db, err := database.NewConnection(cfg.Database)
	if err != nil {
		log.Fatalf("Failed to connect to PostgreSQL: %v", err)
	}
	defer db.Close()

	// Run database migrations
	if err := database.Migrate(db, "migrations"); err != nil {
		log.Fatalf("Failed to run database migrations: %v", err)
	}

	// Initialize MongoDB connection
	mongodb, err := database.ConnectMongoDB(cfg.MongoDB)
	if err != nil {
		log.Fatalf("Failed to connect to MongoDB: %v", err)
	}
	defer mongodb.Close(context.Background())

	// Create MongoDB indexes
	if err := mongodb.CreateIndexes(context.Background()); err != nil {
		log.Fatalf("Failed to create MongoDB indexes: %v", err)
	}

	// Initialize MinIO client
	minioClient, err := minio.New(cfg.MinIO.Endpoint, &minio.Options{
		Creds:  credentials.NewStaticV4(cfg.MinIO.AccessKey, cfg.MinIO.SecretKey, ""),
		Secure: cfg.MinIO.UseSSL,
	})
	if err != nil {
		log.Fatalf("Failed to initialize MinIO client: %v", err)
	}

	// Create bucket if it doesn't exist
	if cfg.MinIO.CreateBucket {
		ctx := context.Background()
		exists, err := minioClient.BucketExists(ctx, cfg.MinIO.BucketName)
		if err != nil {
			log.Fatalf("Failed to check if bucket exists: %v", err)
		}
		if !exists {
			err = minioClient.MakeBucket(ctx, cfg.MinIO.BucketName, minio.MakeBucketOptions{})
			if err != nil {
				log.Fatalf("Failed to create bucket: %v", err)
			}
			log.Printf("Created bucket: %s", cfg.MinIO.BucketName)
		}
	}

	// Initialize repositories
	feedbackRepo := repository.NewFeedbackRepository(db, mongodb)
	attachmentRepo := repository.NewAttachmentRepository(minioClient, cfg.MinIO.BucketName)
	commentRepo := repository.NewCommentRepository(mongodb)

	// Initialize services
	feedbackService := service.NewFeedbackService(feedbackRepo, attachmentRepo)
	commentService := service.NewCommentService(commentRepo)

	// Create gRPC server
	grpcServer := grpc.NewServer(
		grpc.MaxRecvMsgSize(32*1024*1024), // 32MB max message size for file uploads
		grpc.MaxSendMsgSize(32*1024*1024), // 32MB max message size for file downloads
	)

	// Register services
	server.RegisterFeedbackServer(grpcServer, feedbackService)
	server.RegisterCommentServer(grpcServer, commentService)

	// Enable reflection for easier debugging
	reflection.Register(grpcServer)

	// Start server
	listener, err := net.Listen("tcp", ":"+cfg.GRPCPort)
	if err != nil {
		log.Fatalf("Failed to listen on port %s: %v", cfg.GRPCPort, err)
	}

	log.Printf("Starting gRPC server on port %s", cfg.GRPCPort)

	// Graceful shutdown
	go func() {
		if err := grpcServer.Serve(listener); err != nil {
			log.Fatalf("Failed to serve gRPC server: %v", err)
		}
	}()

	// Wait for interrupt signal to gracefully shutdown the server
	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)
	<-quit

	log.Println("Shutting down server...")

	// Graceful shutdown with timeout
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()

	done := make(chan bool, 1)
	go func() {
		grpcServer.GracefulStop()
		done <- true
	}()

	select {
	case <-done:
		log.Println("Server stopped gracefully")
	case <-ctx.Done():
		log.Println("Server shutdown timeout, forcing stop")
		grpcServer.Stop()
	}
}
