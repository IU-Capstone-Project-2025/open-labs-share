package database

import (
	"context"
	"database/sql"
	"fmt"
	"log/slog"
	"time"

	"github.com/IU-Capstone-Project-2025/open-labs-share/services/feedback-service/internal/config"
	_ "github.com/lib/pq" // PostgreSQL driver
)

const (
	maxOpenConns    = 25
	maxIdleConns    = 5
	connMaxLifetime = 5 * time.Minute
)

// NewConnection creates a new database connection
func NewConnection(ctx context.Context, cfg config.DatabaseConfig) (*sql.DB, error) {
	dsn := fmt.Sprintf(
		"host=%s port=%s user=%s password=%s dbname=%s sslmode=disable",
		cfg.Host, cfg.Port, cfg.User, cfg.Password, cfg.DBName,
	)

	db, err := sql.Open("postgres", dsn)
	if err != nil {
		return nil, fmt.Errorf("failed to open database connection: %w", err)
	}

	// Configure connection pool
	db.SetMaxOpenConns(maxOpenConns)
	db.SetMaxIdleConns(maxIdleConns)
	db.SetConnMaxLifetime(connMaxLifetime)

	// Test the connection
	if err := db.PingContext(ctx); err != nil {
		db.Close()
		return nil, fmt.Errorf("failed to ping database: %w", err)
	}

	// Log connection pool stats for monitoring
	go func() {
		ticker := time.NewTicker(30 * time.Second)
		defer ticker.Stop()

		for {
			select {
			case <-ticker.C:
				stats := db.Stats()
				slog.Info("DB Pool Stats",
					"open_connections", stats.OpenConnections,
					"max_open_connections", stats.MaxOpenConnections,
					"idle_connections", stats.Idle,
					"max_idle_connections", maxIdleConns,
					"in_use", stats.InUse,
					"wait_count", stats.WaitCount,
					"wait_duration", stats.WaitDuration,
				)
			case <-ctx.Done():
				return
			}
		}
	}()

	return db, nil
}
