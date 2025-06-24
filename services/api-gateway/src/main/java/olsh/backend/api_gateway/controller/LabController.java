package olsh.backend.api_gateway.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import olsh.backend.api_gateway.annotation.RequireAuth;
import olsh.backend.api_gateway.dto.request.CreateLabRequest;
import olsh.backend.api_gateway.dto.request.GetLabsRequest;
import olsh.backend.api_gateway.dto.response.CreateLabResponse;
import olsh.backend.api_gateway.dto.response.DeleteLabResponse;
import olsh.backend.api_gateway.dto.response.LabListResponse;
import olsh.backend.api_gateway.dto.response.LabResponse;
import olsh.backend.api_gateway.service.LabService;
import org.springdoc.core.annotations.ParameterObject;
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
@RequestMapping("/api/v1/labs")
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "Labs", description = "Endpoints for managing laboratory works with markdown content and supporting files")
@SecurityRequirement(name = "bearerAuth")
public class LabController {

    private final LabService labService;
    private final RequestAttributesExtractor attributesProvider;

    @Autowired
    public LabController(LabService labService, RequestAttributesExtractor attributesProvider) {
        this.labService = labService;
        this.attributesProvider  = attributesProvider;
    }

    @Operation(
        summary = "Create new lab",
        description = "Creates a new laboratory work with markdown file and optional supporting assets. Requires authentication."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Lab created successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CreateLabResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Access denied")
    })
    @RequireAuth
    @PostMapping
    public ResponseEntity<CreateLabResponse> createLab(
            @Valid @ModelAttribute @Parameter(description = "Lab creation data including title, description, markdown file, and optional assets") CreateLabRequest request,
            HttpServletRequest httpRequest) {
        log.debug("Received request to create lab with title: {}", request.getTitle());
        CreateLabResponse response = labService.createLab(request,
                attributesProvider.extractUserIdFromRequest(httpRequest));
        log.debug("Successfully created lab");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
        summary = "Get lab by ID",
        description = "Retrieves detailed information about a specific laboratory work by its ID. Requires authentication."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Lab found and returned successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = LabResponse.class))
        ),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
        @ApiResponse(responseCode = "404", description = "Lab not found")
    })
    @RequireAuth
    @GetMapping("/{lab_id}")
    public ResponseEntity<LabResponse> getLab(
            @Parameter(description = "ID of the lab to retrieve", required = true)
            @PathVariable("lab_id") Long labId,
            HttpServletRequest request) {
        log.debug("Received request to get lab with ID: {}", labId);
        LabResponse response = labService.getLabById(labId);
        log.debug("Successfully retrieved lab data for labId: {}", labId);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Get list of labs",
        description = "Retrieves a paginated list of laboratory works. Requires authentication."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Labs retrieved successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = LabListResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required")
    })
    @RequireAuth
    @GetMapping
    public ResponseEntity<LabListResponse> getLabs(
            @Valid @ParameterObject @Parameter(description = "Pagination parameters") GetLabsRequest request,
            HttpServletRequest httpRequest) {
        log.debug("Received request to get labs with page: {}, limit: {}", request.getPage(), request.getLimit());
        LabListResponse response = labService.getLabs(request);
        log.debug("Successfully retrieved labs list with {} labs", response.getLabs().size());
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Delete lab",
        description = "Deletes a specific laboratory work by its ID. Only the lab owner can delete it. Requires authentication."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Lab deleted successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = DeleteLabResponse.class))
        ),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
        @ApiResponse(responseCode = "403", description = "Forbidden - No access to delete the lab"),
        @ApiResponse(responseCode = "404", description = "Lab not found")
    })
    @RequireAuth
    @DeleteMapping("/{lab_id}")
    public ResponseEntity<DeleteLabResponse> deleteLab(
            @Parameter(description = "ID of the lab to delete", required = true)
            @PathVariable("lab_id") Long labId,
            HttpServletRequest request) {
        log.debug("Received request to delete lab with ID: {}", labId);
        Long userId = attributesProvider.extractUserIdFromRequest(request);
        DeleteLabResponse response = labService.deleteLab(labId, userId);
        log.debug("Successfully deleted lab with ID: {}", labId);
        return ResponseEntity.ok(response);
    }
}

