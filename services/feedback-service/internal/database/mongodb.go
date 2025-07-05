package database

import (
	"context"
	"fmt"
	"time"

	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"

	"github.com/IU-Capstone-Project-2025/open-labs-share/services/feedback-service/internal/config"
)

// MongoDBClient wraps MongoDB client with database and collection
type MongoDBClient struct {
	Client     *mongo.Client
	Database   *mongo.Database
	Collection *mongo.Collection
}

// ConnectMongoDB establishes connection to MongoDB
func ConnectMongoDB(cfg config.MongoDBConfig) (*MongoDBClient, error) {
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()

	// Set client options
	clientOptions := options.Client().ApplyURI(cfg.URI)

	// Connect to MongoDB
	client, err := mongo.Connect(ctx, clientOptions)
	if err != nil {
		return nil, fmt.Errorf("failed to connect to MongoDB: %w", err)
	}

	// Ping the database to verify connection
	if err := client.Ping(ctx, nil); err != nil {
		return nil, fmt.Errorf("failed to ping MongoDB: %w", err)
	}

	database := client.Database(cfg.Database)
	collection := database.Collection(cfg.Collection)

	return &MongoDBClient{
		Client:     client,
		Database:   database,
		Collection: collection,
	}, nil
}

// Close closes the MongoDB connection
func (m *MongoDBClient) Close(ctx context.Context) error {
	return m.Client.Disconnect(ctx)
}

// CreateIndexes creates necessary indexes for the collection
func (m *MongoDBClient) CreateIndexes(ctx context.Context) error {
	// Index for context queries (context + context_id)
	contextIndex := mongo.IndexModel{
		Keys: bson.D{
			{Key: "context", Value: 1},
			{Key: "context_id", Value: 1},
		},
	}

	// Index for parent_id queries (threaded comments)
	parentIndex := mongo.IndexModel{
		Keys: bson.D{
			{Key: "parent_id", Value: 1},
		},
	}

	// Index for user queries
	userIndex := mongo.IndexModel{
		Keys: bson.D{
			{Key: "user_id", Value: 1},
		},
	}

	// Index for timestamp queries
	timestampIndex := mongo.IndexModel{
		Keys: bson.D{
			{Key: "created_at", Value: -1},
		},
	}

	_, err := m.Collection.Indexes().CreateMany(ctx, []mongo.IndexModel{
		contextIndex,
		parentIndex,
		userIndex,
		timestampIndex,
	})
	if err != nil {
		return fmt.Errorf("failed to create indexes: %w", err)
	}

	return nil
}
