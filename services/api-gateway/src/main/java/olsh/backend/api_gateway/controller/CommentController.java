package olsh.backend.api_gateway.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import olsh.backend.api_gateway.annotation.RequireAuth;
import olsh.backend.api_gateway.dto.request.CreateCommentRequest;
import olsh.backend.api_gateway.dto.request.GetCommentsRequest;
import olsh.backend.api_gateway.dto.request.UpdateCommentRequest;
import olsh.backend.api_gateway.dto.response.CommentListResponse;
import olsh.backend.api_gateway.dto.response.CommentResponse;
import olsh.backend.api_gateway.service.CommentService;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Comments", description = "Endpoints for managing comments and replies on labs")
public class CommentController {

    private final CommentService commentService;
    private final RequestAttributesExtractor attributesExtractor;

    @RequireAuth
    @PostMapping("/labs/{labId}/comments")
    @Operation(summary = "Create a comment on a lab", description = "Creates a new top-level comment or a reply to an existing comment.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Comment created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "404", description = "Lab not found")
    })
    public ResponseEntity<CommentResponse> createCommentLab(
            @Parameter(description = "ID of the lab to comment on", required = true) @PathVariable long labId,
            @Valid @RequestBody @Parameter(description = "Request to create comment") CreateCommentRequest request,
            HttpServletRequest httpRequest) {
        long userId = attributesExtractor.extractUserIdFromRequest(httpRequest);
        CommentResponse response = commentService.createComment(labId, userId, request, "lab");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/labs/{labId}/comments")
    @Operation(summary = "List comments for a lab", description = "Retrieves a paginated list of top-level comments for a specific lab.")
    public ResponseEntity<CommentListResponse> getLabCommentsLab(
            @Parameter(description = "ID of the lab", required = true) @PathVariable long labId,
            @Valid @ParameterObject GetCommentsRequest request) {
        CommentListResponse response = commentService.getLabComments(labId, request, "lab");
        return ResponseEntity.ok(response);
    }

    @RequireAuth
    @PostMapping("/articles/{articleId}/comments")
    @Operation(summary = "Create a comment on a article", description = "Creates a new top-level comment or a reply to an existing comment.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Comment created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "404", description = "Lab not found")
    })
    public ResponseEntity<CommentResponse> createCommentArticle(
            @Parameter(description = "ID of the article to comment on", required = true) @PathVariable("articleId") long articleId,
            @Valid @RequestBody @Parameter(description = "Request to create comment") CreateCommentRequest request,
            HttpServletRequest httpRequest) {
        long userId = attributesExtractor.extractUserIdFromRequest(httpRequest);
        CommentResponse response = commentService.createComment(articleId, userId, request, "article");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/articles/{articleId}/comments")
    @Operation(summary = "List comments for a article", description = "Retrieves a paginated list of top-level comments for a specific article.")
    public ResponseEntity<CommentListResponse> getLabCommentsArticle(
            @Parameter(description = "ID of the article", required = true) @PathVariable("articleId") long articleId,
            @Valid @ParameterObject GetCommentsRequest request) {
        CommentListResponse response = commentService.getLabComments(articleId, request, "article");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/comments/{commentId}/replies")
    @Operation(summary = "List replies to a comment", description = "Retrieves a paginated list of replies for a specific parent comment.")
    public ResponseEntity<CommentListResponse> getCommentReplies(
            @Parameter(description = "ID of the parent comment", required = true) @PathVariable String commentId,
            @Valid @ParameterObject GetCommentsRequest request) {
        CommentListResponse response = commentService.getCommentReplies(commentId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/comments/{commentId}")
    @Operation(summary = "Get a single comment", description = "Retrieves a single comment by its ID.")
    public ResponseEntity<CommentResponse> getCommentById(
            @Parameter(description = "ID of the comment", required = true) @PathVariable String commentId) {
        CommentResponse response = commentService.getCommentById(commentId);
        return ResponseEntity.ok(response);
    }

    @RequireAuth
    @PutMapping("/comments/{commentId}")
    @Operation(summary = "Update a comment", description = "Updates the content of an existing comment. Users can only update their own comments.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Comment updated successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not own this comment"),
            @ApiResponse(responseCode = "404", description = "Comment not found")
    })
    public ResponseEntity<CommentResponse> updateComment(
            @Parameter(description = "ID of the comment to update", required = true) @PathVariable String commentId,
            @Valid @RequestBody UpdateCommentRequest request,
            HttpServletRequest httpRequest) {
        long userId = attributesExtractor.extractUserIdFromRequest(httpRequest);
        CommentResponse response = commentService.updateComment(commentId, userId, request);
        return ResponseEntity.ok(response);
    }

    @RequireAuth
    @DeleteMapping("/comments/{commentId}")
    @Operation(summary = "Delete a comment", description = "Deletes a comment. Users can only delete their own comments.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Comment deleted successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not own this comment"),
            @ApiResponse(responseCode = "404", description = "Comment not found")
    })
    public ResponseEntity<Boolean> deleteComment(
            @Parameter(description = "ID of the comment to delete", required = true) @PathVariable String commentId,
            HttpServletRequest httpRequest) {
        long userId = attributesExtractor.extractUserIdFromRequest(httpRequest);
        boolean success = commentService.deleteComment(commentId, userId);
        return ResponseEntity.status(success ? HttpStatus.OK : HttpStatus.NOT_FOUND).body(success);
    }
} 