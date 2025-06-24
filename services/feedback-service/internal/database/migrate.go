package database

import (
	"database/sql"
	"fmt"
	"log"
	"os"
	"path/filepath"
	"sort"
	"strings"

	_ "github.com/lib/pq"
)

// Migrate runs database migrations
func Migrate(db *sql.DB, migrationsPath string) error {
	// Create migrations table if it doesn't exist
	if err := createMigrationsTable(db); err != nil {
		return fmt.Errorf("failed to create migrations table: %w", err)
	}

	// Get list of migration files
	migrationFiles, err := getMigrationFiles(migrationsPath)
	if err != nil {
		return fmt.Errorf("failed to get migration files: %w", err)
	}

	// Get applied migrations
	appliedMigrations, err := getAppliedMigrations(db)
	if err != nil {
		return fmt.Errorf("failed to get applied migrations: %w", err)
	}

	// Apply pending migrations
	for _, migration := range migrationFiles {
		if _, applied := appliedMigrations[migration.Name]; !applied {
			log.Printf("Applying migration: %s", migration.Name)
			if err := applyMigration(db, migration); err != nil {
				return fmt.Errorf("failed to apply migration %s: %w", migration.Name, err)
			}
			log.Printf("Successfully applied migration: %s", migration.Name)
		}
	}

	log.Println("All migrations applied successfully")
	return nil
}

type Migration struct {
	Name string
	SQL  string
}

func createMigrationsTable(db *sql.DB) error {
	query := `
		CREATE TABLE IF NOT EXISTS schema_migrations (
			version VARCHAR(255) PRIMARY KEY,
			applied_at TIMESTAMP DEFAULT NOW()
		)
	`
	_, err := db.Exec(query)
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

func getAppliedMigrations(db *sql.DB) (map[string]bool, error) {
	applied := make(map[string]bool)

	rows, err := db.Query("SELECT version FROM schema_migrations")
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

func applyMigration(db *sql.DB, migration Migration) error {
	tx, err := db.Begin()
	if err != nil {
		return err
	}
	defer func() {
		if err != nil {
			tx.Rollback()
		}
	}()

	// Execute migration SQL
	if _, err = tx.Exec(migration.SQL); err != nil {
		return err
	}

	// Record migration as applied
	if _, err = tx.Exec("INSERT INTO schema_migrations (version) VALUES ($1)", migration.Name); err != nil {
		return err
	}

	return tx.Commit()
}
