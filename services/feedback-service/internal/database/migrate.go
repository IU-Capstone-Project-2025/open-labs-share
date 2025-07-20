package database

import (
	"context"
	"database/sql"
	"fmt"
	"log/slog"
	"os"
	"path/filepath"
	"sort"
	"strings"

	_ "github.com/lib/pq"
)

// Migrate runs database migrations
func Migrate(ctx context.Context, db *sql.DB, migrationsPath string) error {
	// Create migrations table if it doesn't exist
	if err := createMigrationsTable(ctx, db); err != nil {
		return fmt.Errorf("failed to create migrations table: %w", err)
	}

	// Get list of migration files
	migrationFiles, err := getMigrationFiles(migrationsPath)
	if err != nil {
		return fmt.Errorf("failed to get migration files: %w", err)
	}

	// Get applied migrations
	appliedMigrations, err := getAppliedMigrations(ctx, db)
	if err != nil {
		return fmt.Errorf("failed to get applied migrations: %w", err)
	}

	// Apply pending migrations
	for _, migration := range migrationFiles {
		if _, applied := appliedMigrations[migration.Name]; !applied {
			slog.Info("Applying migration", "migration", migration.Name)
			if err := applyMigration(ctx, db, migration); err != nil {
				return fmt.Errorf("failed to apply migration %s: %w", migration.Name, err)
			}
			slog.Info("Successfully applied migration", "migration", migration.Name)
		}
	}

	slog.Info("All migrations applied successfully")
	return nil
}

type Migration struct {
	Name string
	SQL  string
}

func createMigrationsTable(ctx context.Context, db *sql.DB) error {
	query := `
		CREATE TABLE IF NOT EXISTS schema_migrations (
			version VARCHAR(255) PRIMARY KEY,
			applied_at TIMESTAMP DEFAULT NOW()
		)
	`
	_, err := db.ExecContext(ctx, query)
	return err
}

func getMigrationFiles(migrationsPath string) ([]Migration, error) {
	var migrations []Migration

	err := filepath.Walk(migrationsPath, func(path string, info os.FileInfo, err error) error {
		if err != nil {
			return err
		}

		if info.IsDir() {
			return nil
		}

		// Only process .up.sql files
		if strings.HasSuffix(info.Name(), ".up.sql") {
			content, err := os.ReadFile(path)
			if err != nil {
				return fmt.Errorf("failed to read migration file %s: %w", path, err)
			}

			migrations = append(migrations, Migration{
				Name: strings.TrimSuffix(info.Name(), ".up.sql"),
				SQL:  string(content),
			})
		}

		return nil
	})

	if err != nil {
		return nil, err
	}

	// Sort migrations by name to ensure proper order
	sort.Slice(migrations, func(i, j int) bool {
		return migrations[i].Name < migrations[j].Name
	})

	return migrations, nil
}

func getAppliedMigrations(ctx context.Context, db *sql.DB) (map[string]bool, error) {
	applied := make(map[string]bool)

	rows, err := db.QueryContext(ctx, "SELECT version FROM schema_migrations")
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	for rows.Next() {
		var version string
		if err := rows.Scan(&version); err != nil {
			return nil, err
		}
		applied[version] = true
	}

	return applied, rows.Err()
}

func applyMigration(ctx context.Context, db *sql.DB, migration Migration) error {
	tx, err := db.BeginTx(ctx, nil)
	if err != nil {
		return err
	}
	defer func() {
		if err != nil {
			tx.Rollback()
		}
	}()

	// Execute migration SQL
	if _, err = tx.ExecContext(ctx, migration.SQL); err != nil {
		return err
	}

	// Record migration as applied
	if _, err = tx.ExecContext(ctx, "INSERT INTO schema_migrations (version) VALUES ($1)", migration.Name); err != nil {
		return err
	}

	return tx.Commit()
}
