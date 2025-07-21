package olsh.backend.api_gateway.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import olsh.backend.api_gateway.annotation.RequireAuth;
import olsh.backend.api_gateway.dto.request.CreateSubmissionRequest;
import olsh.backend.api_gateway.dto.response.*;
import olsh.backend.api_gateway.service.SubmissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@Slf4j
@RestController
@RequestMapping("/api/v1/submissions")
@CrossOrigin(origins = {"http://localhost:5173", "https://open-labs-share.online"}, allowCredentials = "true", maxAge = 3600)
@Tag(name = "Submissions", description = "Endpoints for managing lab submissions with text comments and file " +
        "attachments")
@SecurityRequirement(name = "bearerAuth")
public class SubmissionController {

    private final SubmissionService submissionService;
    private final RequestAttributesExtractor attributesProvider;

    @Autowired
    public SubmissionController(SubmissionService submissionService, RequestAttributesExtractor attributesProvider) {
        this.submissionService = submissionService;
        this.attributesProvider = attributesProvider;
    }

    @Operation(
            summary = "Create new submission",
            description = "Creates a new submission with text comment and optional file attachments. Requires " +
                    "authentication."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Submission created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation =
                            CreateSubmissionResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
            @ApiResponse(responseCode = "404", description = "Lab not found")
    })
    @RequireAuth
    @PostMapping
    public ResponseEntity<CreateSubmissionResponse> createSubmission(
            @Valid @ModelAttribute CreateSubmissionRequest request,
            HttpServletRequest httpRequest) {
        log.debug("Received request to create submission for lab ID: {}", request.getLabId());

        Long userId = attributesProvider.extractUserIdFromRequest(httpRequest);
        CreateSubmissionResponse response = submissionService.createSubmission(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @Operation(
            summary = "Get submission by ID",
            description = "Retrieves detailed information about a specific submission including its assets. Requires " +
                    "authentication."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Submission found and returned successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation =
                            SubmissionResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
            @ApiResponse(responseCode = "404", description = "Submission not found")
    })
    @RequireAuth
    @GetMapping("/{submission_id}")
    public ResponseEntity<SubmissionResponse> getSubmission(
            @Parameter(description = "ID of the submission to retrieve", required = true)
            @PathVariable("submission_id") Long submissionId,
            HttpServletRequest request) {

        log.debug("Received request to get submission with ID: {}", submissionId);

        SubmissionResponse response = submissionService.getById(submissionId);
        log.debug("Successfully retrieved submission data for submissionId: {}", submissionId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get submissions by lab ID",
            description = "Retrieves all submissions for a specific lab including their assets. Requires " +
                    "authentication."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Submissions retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation =
                            SubmissionListResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
            @ApiResponse(responseCode = "404", description = "Lab not found")
    })
    @RequireAuth
    @GetMapping("/lab/{lab_id}")
    public ResponseEntity<SubmissionListResponse> getSubmissionsByLab(
            @PathVariable("lab_id") Long labId,
            @RequestParam(defaultValue = "1") @Schema(description = "Номер страницы", example = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") @Schema(description = "Размер страницы", example = "20") Integer pageSize,
            HttpServletRequest request) {

        log.debug("Received request to get submissions for lab ID: {} (page: {}, size: {})",
                labId, pageNum, pageSize);

        SubmissionListResponse response = submissionService.getByLabId(labId, pageNum, pageSize);
        log.debug("Successfully retrieved {} submissions for lab ID: {}",
                response.getSubmissions().size(), labId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Delete submission",
            description = "Deletes a specific submission by its ID. Only the submission owner can delete it. Requires" +
                    " authentication."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Submission deletion response",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation =
                            DeleteSubmissionResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - No access to delete the submission"),
            @ApiResponse(responseCode = "404", description = "Submission not found")
    })
    @RequireAuth
    @DeleteMapping("/{submission_id}")
    public ResponseEntity<DeleteSubmissionResponse> deleteSubmission(
            @Parameter(description = "ID of the submission to delete", required = true)
            @PathVariable("submission_id") Long submissionId,
            HttpServletRequest request) {

        log.debug("Received request to delete submission with ID: {}", submissionId);

        Long userId = attributesProvider.extractUserIdFromRequest(request);
        DeleteSubmissionResponse response = submissionService.deleteSubmission(submissionId, userId);
        log.debug("Successfully processed deletion request for submission ID: {}", submissionId);
        return ResponseEntity.ok(response);
    }


    @Operation(
            summary = "Get user's own submissions",
            description = "Returns a paginated list of all submissions made by the authenticated user, including " +
                    "their attachments. Authentication required."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "List of submissions successfully retrieved",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation =
                            SubmissionListResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required")
    })
    @RequireAuth
    @GetMapping("/my")
    public ResponseEntity<SubmissionListResponse> getMySubmissions(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") @Parameter(description = "Page number (starts from 1)", example = "1") Integer page,
            @RequestParam(defaultValue = "20") @Parameter(description = "Page size", example = "20") Integer limit
    ) {
        Long userId = attributesProvider.extractUserIdFromRequest(request);
        log.debug("Received request to get submissions for user with ID: {} (page: {}, limit: {})", userId, page,
                limit);
        SubmissionListResponse response = submissionService.getByUserId(userId, page, limit);
        log.debug("Successfully retrieved {} submissions for user with ID: {}", response.getSubmissions().size(),
                userId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get submissions for review",
            description = "Retrieves a paginated list of submissions that require review. Requires authentication."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Submissions for review retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation =
                            SubmissionListResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required")
    })
    @RequireAuth
    @GetMapping("/review")
    public ResponseEntity<SubmissionListResponse> getSubmissionsForReview(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") @Parameter(description = "Page number (starts from 1)", example = "1") Integer page,
            @RequestParam(defaultValue = "20") @Parameter(description = "Page size", example = "20") Integer limit
    ){
        Long userId = attributesProvider.extractUserIdFromRequest(request);
        log.debug("Received request to get submissions for review (page: {}, limit: {})", page, limit);
        SubmissionListResponse response = submissionService.getForReview(userId, page, limit);
        log.debug("Successfully retrieved {} submissions for review", response.getSubmissions().size());
        return ResponseEntity.ok(response);
    }
}

