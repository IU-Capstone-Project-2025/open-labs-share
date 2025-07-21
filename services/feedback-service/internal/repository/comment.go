package repository

import (
	"context"
	"fmt"
	"time"

	"github.com/IU-Capstone-Project-2025/open-labs-share/services/feedback-service/internal/database"
	"github.com/IU-Capstone-Project-2025/open-labs-share/services/feedback-service/internal/models"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"
)

type CommentRepositoryTx struct {
	CommentRepository
}

// commentRepository implements CommentRepository using MongoDB
type commentRepository struct {
	mongodb        *database.MongoDBClient
	collectionName string
}

func (r *commentRepository) WithTransaction(ctx context.Context, fn func(CommentTxRepository) error) error {
	return r.mongodb.WithTransaction(ctx, func(sc mongo.SessionContext) error {
		repo := &commentRepository{
			mongodb: &database.MongoDBClient{
				Client:   r.mongodb.Client,
				Database: r.mongodb.Database,
				Session:  sc,
			},
			collectionName: r.collectionName,
		}

		return fn(repo)
	})
}

// NewCommentRepository creates a new comment repository
func NewCommentRepository(mongodb *database.MongoDBClient, collectionName string) CommentRepository {
	return &commentRepository{
		mongodb:        mongodb,
		collectionName: collectionName,
	}
}

func (r *commentRepository) collection() *mongo.Collection {
	if r.mongodb.Session != nil {
		return r.mongodb.Session.Client().Database(r.mongodb.Database.Name()).Collection(r.collectionName)
	}
	return r.mongodb.Database.Collection(r.collectionName)
}

// Create creates a new comment
func (r *commentRepository) Create(ctx context.Context, comment *models.Comment) error {
	comment.ID = primitive.NewObjectID()
	comment.CreatedAt = time.Now().UTC()
	comment.UpdatedAt = comment.CreatedAt

	_, err := r.collection().InsertOne(ctx, comment)
	if err != nil {
		return fmt.Errorf("failed to create comment: %w", err)
	}

	return nil
}

// GetByID retrieves a comment by ID
func (r *commentRepository) GetByID(ctx context.Context, id string) (*models.Comment, error) {
	objectID, err := primitive.ObjectIDFromHex(id)
	if err != nil {
		return nil, fmt.Errorf("invalid comment ID: %w", err)
	}

	var comment models.Comment
	err = r.collection().FindOne(ctx, bson.M{"_id": objectID}).Decode(&comment)
	if err != nil {
		if err == mongo.ErrNoDocuments {
			return nil, fmt.Errorf("comment not found")
		}
		return nil, fmt.Errorf("failed to get comment: %w", err)
	}

	return &comment, nil
}

// Update updates an existing comment
func (r *commentRepository) Update(ctx context.Context, comment *models.Comment) error {
	comment.UpdatedAt = time.Now().UTC()

	filter := bson.M{"_id": comment.ID}
	update := bson.M{
		"$set": bson.M{
			"content":    comment.Content,
			"updated_at": comment.UpdatedAt,
		},
	}

	result, err := r.collection().UpdateOne(ctx, filter, update)
	if err != nil {
		return fmt.Errorf("failed to update comment: %w", err)
	}

	if result.MatchedCount == 0 {
		return fmt.Errorf("comment not found")
	}

	return nil
}

// Delete deletes a comment and all its replies
func (r *commentRepository) Delete(ctx context.Context, id string) error {
	objectID, err := primitive.ObjectIDFromHex(id)
	if err != nil {
		return fmt.Errorf("invalid comment ID: %w", err)
	}

	// Start a transaction
	return r.WithTransaction(ctx, func(txRepo CommentTxRepository) error {
		// Delete all replies first (recursive)
		if err := txRepo.DeleteReplies(ctx, id); err != nil {
			return fmt.Errorf("failed to delete replies: %w", err)
		}

		// Delete the comment itself
		result, err := r.collection().DeleteOne(ctx, bson.M{"_id": objectID})
		if err != nil {
			return fmt.Errorf("failed to delete comment: %w", err)
		}

		if result.DeletedCount == 0 {
			return fmt.Errorf("comment not found")
		}
		return nil
	})
}

// DeleteReplies deletes all replies to a specific comment (iterative approach)
func (r *commentRepository) DeleteReplies(ctx context.Context, parentID string) error {
	// Get all descendant IDs iteratively
	descendantIDs, err := r.getAllDescendantIDs(ctx, parentID)
	if err != nil {
		return fmt.Errorf("failed to get descendant IDs: %w", err)
	}

	// If no descendants, nothing to delete
	if len(descendantIDs) == 0 {
		return nil
	}

	// Delete all descendants in a single operation
	filter := bson.M{"_id": bson.M{"$in": descendantIDs}}
	_, err = r.collection().DeleteMany(ctx, filter)
	if err != nil {
		return fmt.Errorf("failed to delete replies: %w", err)
	}

	return nil
}

// getAllDescendantIDs iteratively collects all descendant comment IDs using an aggregation pipeline
func (r *commentRepository) getAllDescendantIDs(ctx context.Context, parentID string) ([]primitive.ObjectID, error) {
	objectID, err := primitive.ObjectIDFromHex(parentID)
	if err != nil {
		return nil, fmt.Errorf("invalid parent ID: %w", err)
	}

	pipeline := mongo.Pipeline{
		{{"$match", bson.M{"_id": objectID}}},
		{{"$graphLookup", bson.M{
			"from":             r.collectionName,
			"startWith":        "$_id",
			"connectFromField": "_id",
			"connectToField":   "parent_id",
			"as":               "descendants",
		}}},
		{{"$project", bson.M{
			"descendant_ids": "$descendants._id",
		}}},
	}

	cursor, err := r.collection().Aggregate(ctx, pipeline)
	if err != nil {
		return nil, fmt.Errorf("failed to execute graphLookup: %w", err)
	}
	defer cursor.Close(ctx)

	var result struct {
		DescendantIDs []primitive.ObjectID `bson:"descendant_ids"`
	}

	if cursor.Next(ctx) {
		if err := cursor.Decode(&result); err != nil {
			return nil, fmt.Errorf("failed to decode graphLookup result: %w", err)
		}
	}

	return result.DescendantIDs, nil
}

// ListByContext lists comments by content ID
func (r *commentRepository) ListByContext(ctx context.Context, filter models.CommentFilter) ([]*models.Comment, int32, error) {
	// Build base filter
	mongoFilter := bson.M{
		"content_id": filter.ContentID,
		"type":       filter.Type,
	}

	// Add parent filter (null for top-level comments, specific ID for replies)
	if filter.ParentID != nil {
		mongoFilter["parent_id"] = *filter.ParentID
	} else {
		// For top-level comments, look for documents where parent_id doesn't exist, is null, or is empty string
		mongoFilter["$or"] = []bson.M{
			{"parent_id": bson.M{"$exists": false}},
			{"parent_id": nil},
			{"parent_id": ""},
		}
	}

	// Get total count
	totalCount, err := r.collection().CountDocuments(ctx, mongoFilter)
	if err != nil {
		return nil, 0, fmt.Errorf("failed to count comments: %w", err)
	}

	// Set up find options with pagination and sorting
	findOptions := options.Find()
	findOptions.SetSort(bson.D{{"created_at", -1}}) // Newest first
	findOptions.SetSkip(int64((filter.Page - 1) * filter.Limit))
	findOptions.SetLimit(int64(filter.Limit))

	// Find comments
	cursor, err := r.collection().Find(ctx, mongoFilter, findOptions)
	if err != nil {
		return nil, 0, fmt.Errorf("failed to find comments: %w", err)
	}
	defer cursor.Close(ctx)

	var comments []*models.Comment
	for cursor.Next(ctx) {
		var comment models.Comment
		if err := cursor.Decode(&comment); err != nil {
			return nil, 0, fmt.Errorf("failed to decode comment: %w", err)
		}
		comments = append(comments, &comment)
	}

	if err := cursor.Err(); err != nil {
		return nil, 0, fmt.Errorf("cursor error: %w", err)
	}

	return comments, int32(totalCount), nil
}

// ListReplies lists replies to a specific comment
func (r *commentRepository) ListReplies(ctx context.Context, parentID string, page, limit int32) ([]*models.Comment, int32, error) {
	// Build filter for replies
	mongoFilter := bson.M{"parent_id": parentID}

	// Get total count
	totalCount, err := r.collection().CountDocuments(ctx, mongoFilter)
	if err != nil {
		return nil, 0, fmt.Errorf("failed to count replies: %w", err)
	}

	// Set up find options with pagination and sorting
	findOptions := options.Find()
	findOptions.SetSort(bson.D{{"created_at", 1}}) // Oldest first for replies
	findOptions.SetSkip(int64((page - 1) * limit))
	findOptions.SetLimit(int64(limit))

	// Find replies
	cursor, err := r.collection().Find(ctx, mongoFilter, findOptions)
	if err != nil {
		return nil, 0, fmt.Errorf("failed to find replies: %w", err)
	}
	defer cursor.Close(ctx)

	var comments []*models.Comment
	for cursor.Next(ctx) {
		var comment models.Comment
		if err := cursor.Decode(&comment); err != nil {
			return nil, 0, fmt.Errorf("failed to decode reply: %w", err)
		}
		comments = append(comments, &comment)
	}

	if err := cursor.Err(); err != nil {
		return nil, 0, fmt.Errorf("cursor error: %w", err)
	}

	return comments, int32(totalCount), nil
}
