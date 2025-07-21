package database

import (
	"context"
	"fmt"

	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"

	"github.com/IU-Capstone-Project-2025/open-labs-share/services/feedback-service/internal/config"
)

// MongoDBClient wraps MongoDB client with database and collection
type MongoDBClient struct {
	Client   *mongo.Client
	Database *mongo.Database
	Session  mongo.SessionContext
}

// ConnectMongoDB establishes connection to MongoDB
func ConnectMongoDB(ctx context.Context, cfg config.MongoDBConfig) (*MongoDBClient, error) {
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

	return &MongoDBClient{
		Client:   client,
		Database: database,
	}, nil
}

// Close closes the MongoDB connection
func (m *MongoDBClient) Close(ctx context.Context) error {
	return m.Client.Disconnect(ctx)
}

// CreateIndexes creates necessary indexes for the collection
func (m *MongoDBClient) CreateIndexes(ctx context.Context, collectionName string) error {
	collection := m.Database.Collection(collectionName)

	// Index for content_id queries
	contentIDIndex := mongo.IndexModel{
		Keys: bson.D{
			{Key: "content_id", Value: 1},
			{Key: "type", Value: 1},
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

	_, err := collection.Indexes().CreateMany(ctx, []mongo.IndexModel{
		contentIDIndex,
		parentIndex,
		userIndex,
		timestampIndex,
	})
	if err != nil {
		return fmt.Errorf("failed to create indexes: %w", err)
	}

	return nil
}

func (m *MongoDBClient) WithTransaction(ctx context.Context, fn func(mongo.SessionContext) error) error {
	session, err := m.Client.StartSession()
	if err != nil {
		return fmt.Errorf("failed to start session: %w", err)
	}
	defer session.EndSession(ctx)

	_, err = session.WithTransaction(ctx, func(sc mongo.SessionContext) (interface{}, error) {
		return nil, fn(sc)
	})

	if err != nil {
		return fmt.Errorf("transaction failed: %w", err)
	}

	return nil
}
