package database

import (
	"database/sql"
	"fmt"
	"log"
	"time"

	"github.com/Ravwvil/feedback/internal/config"
	_ "github.com/lib/pq" // PostgreSQL driver
)

// NewConnection creates a new database connection
func NewConnection(cfg config.DatabaseConfig) (*sql.DB, error) {
	dsn := fmt.Sprintf(
		"host=%s port=%s user=%s password=%s dbname=%s sslmode=disable",
		cfg.Host, cfg.Port, cfg.User, cfg.Password, cfg.DBName,
	)

	db, err := sql.Open("postgres", dsn)
	if err != nil {
		return nil, fmt.Errorf("failed to open database connection: %w", err)
	}

	// Configure connection pool
	db.SetMaxOpenConns(25)                 // Maximum number of open connections
	db.SetMaxIdleConns(5)                  // Maximum number of idle connections
	db.SetConnMaxLifetime(5 * time.Minute) // Maximum connection lifetime

	// Test the connection
	if err := db.Ping(); err != nil {
		db.Close()
		return nil, fmt.Errorf("failed to ping database: %w", err)
	}

	// Log connection pool stats for monitoring
	go func() {
		ticker := time.NewTicker(30 * time.Second)
		defer ticker.Stop()

		for range ticker.C {
			stats := db.Stats()
			log.Printf("DB Pool Stats - Open: %d/%d, Idle: %d/%d, InUse: %d, WaitCount: %d, WaitDuration: %v",
				stats.OpenConnections, stats.MaxOpenConnections,
				stats.Idle, 5, // MaxIdleConns не выводится в stats, указываем константу
				stats.InUse,
				stats.WaitCount,
				stats.WaitDuration,
			)
		}
	}()

	return db, nil
}
