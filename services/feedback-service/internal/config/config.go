package config

import (
	"fmt"
	"os"
	"strconv"
)

// Constants for attachment limits
const (
	MaxAttachmentsPerFeedback = 5
)

// Config represents the application configuration
type Config struct {
	GRPCPort string
	Database DatabaseConfig
	MongoDB  MongoDBConfig
	MinIO    MinIOConfig
}

// DatabaseConfig represents PostgreSQL database configuration (for feedback metadata)
type DatabaseConfig struct {
	Host     string
	Port     string
	User     string
	Password string
	DBName   string
}

// MongoDBConfig represents MongoDB configuration (for comments and feedback content)
type MongoDBConfig struct {
	URI        string
	Database   string
	Collection string
}

// MinIOConfig represents MinIO configuration
type MinIOConfig struct {
	Endpoint     string
	AccessKey    string
	SecretKey    string
	BucketName   string
	UseSSL       bool
	CreateBucket bool
}

// Load loads configuration from environment variables
func Load() (*Config, error) {
	cfg := &Config{
		GRPCPort: getEnv("GRPC_PORT", "9090"),
		Database: DatabaseConfig{
			Host:     getEnv("POSTGRES_HOST", "localhost"),
			Port:     getEnv("POSTGRES_PORT", "5432"),
			User:     getEnv("POSTGRES_USER", "feedback_user"),
			Password: getEnv("POSTGRES_PASSWORD", "feedback_password"),
			DBName:   getEnv("POSTGRES_DB", "feedback_db"),
		},
		MongoDB: MongoDBConfig{
			URI:        getEnv("MONGODB_URI", "mongodb://localhost:27017"),
			Database:   getEnv("MONGODB_DATABASE", "feedback"),
			Collection: getEnv("MONGODB_COLLECTION", "feedback_content"),
		},
		MinIO: MinIOConfig{
			Endpoint:     getEnv("MINIO_ENDPOINT", "localhost:9000"),
			AccessKey:    getEnv("MINIO_ACCESS_KEY", "minioadmin"),
			SecretKey:    getEnv("MINIO_SECRET_KEY", "minioadmin"),
			BucketName:   getEnv("MINIO_BUCKET_NAME", "feedback"),
			UseSSL:       getEnvBool("MINIO_USE_SSL", false),
			CreateBucket: getEnvBool("MINIO_CREATE_BUCKET", true),
		},
	}

	if err := cfg.validate(); err != nil {
		return nil, fmt.Errorf("configuration validation failed: %w", err)
	}

	return cfg, nil
}

// validate validates the configuration
func (c *Config) validate() error {
	if c.GRPCPort == "" {
		return fmt.Errorf("GRPC_PORT is required")
	}
	if c.Database.Host == "" {
		return fmt.Errorf("POSTGRES_HOST is required")
	}
	if c.Database.User == "" {
		return fmt.Errorf("POSTGRES_USER is required")
	}
	if c.Database.Password == "" {
		return fmt.Errorf("POSTGRES_PASSWORD is required")
	}
	if c.Database.DBName == "" {
		return fmt.Errorf("POSTGRES_DB is required")
	}
	if c.MongoDB.URI == "" {
		return fmt.Errorf("MONGODB_URI is required")
	}
	if c.MongoDB.Database == "" {
		return fmt.Errorf("MONGODB_DATABASE is required")
	}
	if c.MongoDB.Collection == "" {
		return fmt.Errorf("MONGODB_COLLECTION is required")
	}
	if c.MinIO.Endpoint == "" {
		return fmt.Errorf("MINIO_ENDPOINT is required")
	}
	if c.MinIO.AccessKey == "" {
		return fmt.Errorf("MINIO_ACCESS_KEY is required")
	}
	if c.MinIO.SecretKey == "" {
		return fmt.Errorf("MINIO_SECRET_KEY is required")
	}
	if c.MinIO.BucketName == "" {
		return fmt.Errorf("MINIO_BUCKET_NAME is required")
	}
	return nil
}

// getEnv gets an environment variable with a default value
func getEnv(key, defaultValue string) string {
	if value := os.Getenv(key); value != "" {
		return value
	}
	return defaultValue
}

// getEnvBool gets a boolean environment variable with a default value
func getEnvBool(key string, defaultValue bool) bool {
	if value := os.Getenv(key); value != "" {
		if parsed, err := strconv.ParseBool(value); err == nil {
			return parsed
		}
	}
	return defaultValue
}
