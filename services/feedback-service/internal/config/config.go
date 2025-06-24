package config

import (
	"fmt"
	"os"
	"strconv"
)

// Config represents the application configuration
type Config struct {
	GRPCPort string
	Database DatabaseConfig
	MinIO    MinIOConfig
}

// DatabaseConfig represents database configuration
type DatabaseConfig struct {
	Host     string
	Port     string
	User     string
	Password string
	DBName   string
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
			Host:     getEnv("DB_HOST", "localhost"),
			Port:     getEnv("DB_PORT", "5432"),
			User:     getEnv("DB_USER", "feedback_user"),
			Password: getEnv("DB_PASSWORD", "feedback_password"),
			DBName:   getEnv("DB_NAME", "feedback_db"),
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
		return fmt.Errorf("DB_HOST is required")
	}
	if c.Database.User == "" {
		return fmt.Errorf("DB_USER is required")
	}
	if c.Database.Password == "" {
		return fmt.Errorf("DB_PASSWORD is required")
	}
	if c.Database.DBName == "" {
		return fmt.Errorf("DB_NAME is required")
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
