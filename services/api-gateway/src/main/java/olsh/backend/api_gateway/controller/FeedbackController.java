package olsh.backend.api_gateway.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import olsh.backend.api_gateway.annotation.RequireAuth;
import olsh.backend.api_gateway.dto.request.CreateFeedbackRequest;
import olsh.backend.api_gateway.dto.response.DeleteFeedbackResponse;
import olsh.backend.api_gateway.dto.response.FeedbackListResponse;
import olsh.backend.api_gateway.dto.response.FeedbackResponse;
import olsh.backend.api_gateway.exception.ErrorResponse;
import olsh.backend.api_gateway.service.FeedbackService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/feedback")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "https://open-labs-share.online"}, allowCredentials = "true", maxAge = 3600)
@Tag(name = "Feedback", description = "Feedback management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class FeedbackController {

    private final FeedbackService feedbackService;
    private final RequestAttributesExtractor attributesProvider;

    @Operation(summary = "Create new feedback", description = "Creates a new feedback for a submission with optional " +
            "file attachments")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Feedback created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema =
            @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema =
            @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema =
            @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequireAuth
    public ResponseEntity<FeedbackResponse> createFeedback(
            @Valid @ModelAttribute CreateFeedbackRequest request,
            HttpServletRequest httpRequest) {
        Long userId = attributesProvider.extractUserIdFromRequest(httpRequest);
        FeedbackResponse response = feedbackService.createFeedback(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Delete feedback", description = "Deletes a feedback by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Feedback deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema =
            @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema =
            @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Feedback not found", content = @Content(schema =
            @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{feedbackId}")
    @RequireAuth
    public ResponseEntity<DeleteFeedbackResponse> deleteFeedback(
            @PathVariable String feedbackId,
            HttpServletRequest httpRequest) {
        Long userId = attributesProvider.extractUserIdFromRequest(httpRequest);
        DeleteFeedbackResponse response = feedbackService.deleteFeedback(feedbackId, userId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get my feedback for submission", description = "Retrieves feedback for a specific submission")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Feedback retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema =
            @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Feedback not found", content = @Content(schema =
            @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/my/{submissionId}")
    @RequireAuth
    public ResponseEntity<FeedbackResponse> getMyFeedback(
            @PathVariable Long submissionId,
            HttpServletRequest httpRequest) {
        Long userId = attributesProvider.extractUserIdFromRequest(httpRequest);
        FeedbackResponse response = feedbackService.getStudentFeedback(userId, submissionId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "List my feedbacks", description = "Lists all feedbacks for the authenticated student with " +
            "pagination")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Feedbacks retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema =
            @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/my")
    @RequireAuth
    public ResponseEntity<FeedbackListResponse> listMyFeedbacks(
            @Parameter(description = "Page number (1-based)", example = "1") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "Items per page", example = "20") @RequestParam(defaultValue = "20") Integer limit,
            HttpServletRequest httpRequest) {
        Long userId = attributesProvider.extractUserIdFromRequest(httpRequest);
        FeedbackListResponse response = feedbackService.listStudentFeedbacks(userId, null, page, limit);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get feedback by ID", description = "Retrieves a specific feedback by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Feedback retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema =
            @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Feedback not found", content = @Content(schema =
            @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{feedbackId}")
    @RequireAuth
    public ResponseEntity<FeedbackResponse> getFeedbackById(
            @PathVariable String feedbackId) {
        FeedbackResponse response = feedbackService.getFeedback(feedbackId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "List student feedbacks", description = "Lists all feedbacks for a specific student")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Feedbacks retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema =
            @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema =
            @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/student/{studentId}")
    @RequireAuth
    public ResponseEntity<FeedbackListResponse> getStudentFeedbacks(
            @PathVariable Long studentId,
            @Parameter(description = "Page number (1-based)",
                    example = "1") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "Items per page",
                    example = "20") @RequestParam(defaultValue = "20") Integer limit) {
        FeedbackListResponse response = feedbackService.listStudentFeedbacks(studentId, null, page, limit);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "List reviewer feedbacks", description = "Lists all feedbacks created by the authenticated " +
            "reviewer with optional submission filter")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Feedbacks retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema =
            @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema =
            @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/reviewer/{reviewerId}")
    @RequireAuth
    public ResponseEntity<FeedbackListResponse> listReviewerFeedbacks(
            @PathVariable Long reviewerId,
            @Parameter(description = "Page number (1-based)", example = "1") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "Items per page", example = "20") @RequestParam(defaultValue = "20") Integer limit,
            @Parameter(description = "Optional submission ID filter") @RequestParam(required = false) Long submissionId) {
        FeedbackListResponse response = feedbackService.listReviewerFeedbacks(reviewerId, submissionId, page, limit);
        return ResponseEntity.ok(response);
    }
} 