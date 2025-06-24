package main

import (
	"context"
	"log"
	"net"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/Ravwvil/feedback/internal/config"
	"github.com/Ravwvil/feedback/internal/database"
	"github.com/Ravwvil/feedback/internal/grpc/server"
	"github.com/Ravwvil/feedback/internal/repository"
	"github.com/Ravwvil/feedback/internal/service"
	"github.com/Ravwvil/feedback/internal/storage"
	"google.golang.org/grpc"
	"google.golang.org/grpc/reflection"
)

func main() {
	// Load configuration
	cfg, err := config.Load()
	if err != nil {
		log.Fatalf("Failed to load configuration: %v", err)
	}
	// Initialize database connection
	db, err := database.NewConnection(cfg.Database)
	if err != nil {
		log.Fatalf("Failed to connect to database: %v", err)
	}
	defer db.Close()

	// Run database migrations
	if err := database.Migrate(db, "migrations"); err != nil {
		log.Fatalf("Failed to run database migrations: %v", err)
	}

	// Initialize MinIO storage
	minioStorage, err := storage.NewMinIOStorage(cfg.MinIO)
	if err != nil {
		log.Fatalf("Failed to initialize MinIO storage: %v", err)
	}

	// Initialize repositories
	feedbackRepo := repository.NewFeedbackRepository(db)
	assetRepo := repository.NewAssetRepository(db)
	commentRepo := repository.NewCommentRepository(db)

	// Initialize services
	feedbackService := service.NewFeedbackService(feedbackRepo, assetRepo, minioStorage)
	commentService := service.NewCommentService(commentRepo)

	// Create gRPC server
	grpcServer := grpc.NewServer(
		grpc.MaxRecvMsgSize(32*1024*1024), // 32MB max message size for file uploads
		grpc.MaxSendMsgSize(32*1024*1024), // 32MB max message size for file downloads
	)

	// Register services
	server.RegisterFeedbackServer(grpcServer, feedbackService, commentService)

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
