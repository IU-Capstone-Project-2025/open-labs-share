package olsh.backend.api_gateway.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import olsh.backend.api_gateway.annotation.RequireAuth;
import olsh.backend.api_gateway.dto.request.LabCreateRequest;
import olsh.backend.api_gateway.dto.request.LabsGetRequest;
import olsh.backend.api_gateway.dto.response.*;
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
@CrossOrigin(origins = {"http://localhost:5173", "https://open-labs-share.online"}, allowCredentials = "true", maxAge = 3600)
@Tag(name = "Labs", description = "Endpoints for managing laboratory works with markdown content and supporting files")
@SecurityRequirement(name = "bearerAuth")
public class LabController {

    private final LabService labService;
    private final RequestAttributesExtractor attributesProvider;

    @Autowired
    public LabController(LabService labService, RequestAttributesExtractor attributesProvider) {
        this.labService = labService;
        this.attributesProvider = attributesProvider;
    }

    @Operation(
            summary = "Create new lab",
            description = "Creates a new laboratory work with markdown file and optional supporting assets. Requires " +
                    "authentication."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Lab created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation =
                            LabCreateResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Access denied")
    })
    @RequireAuth
    @PostMapping
    public ResponseEntity<?> createLab(
            @Valid @ModelAttribute LabCreateRequest request,
            HttpServletRequest httpRequest) {
        log.debug("Received request to create lab with title: {}", request.getTitle());
        LabCreateResponse response = labService.createLab(request,
                attributesProvider.extractUserIdFromRequest(httpRequest));
        log.debug("Successfully created lab");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Get lab by ID",
            description = "Retrieves detailed information about a specific laboratory work by its ID. Requires " +
                    "authentication."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Lab found and returned successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation =
                            LabResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
            @ApiResponse(responseCode = "404", description = "Lab not found")
    })
    @RequireAuth
    @GetMapping("/{lab_id}")
    public ResponseEntity<LabAndTagsResponse> getLab(
            @Parameter(description = "ID of the lab to retrieve", required = true)
            @PathVariable("lab_id") Long labId,
            HttpServletRequest request) {
        log.debug("Received request to get lab with ID: {}", labId);
        LabAndTagsResponse response = labService.getLabById(labId);
        log.debug("Successfully retrieved lab data for labId: {}", labId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get my labs",
            description = "Retrieves a paginated list of laboratory works created by the current user. Requires " +
                    "authentication."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User's labs retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation =
                            LabListResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required")
    })
    @RequireAuth
    @GetMapping("/my")
    public ResponseEntity<LabListResponse> getMyLabs(
            HttpServletRequest httpRequest,
            @RequestParam(defaultValue = "1") @Parameter(description = "Page number (starts from 1)", example = "1") Integer page,
            @RequestParam(defaultValue = "20") @Parameter(description = "Page size", example = "20") Integer limit) {
        log.debug("Received request to get my labs with page: {}, limit: {}",
                page, limit);

        Long userId = attributesProvider.extractUserIdFromRequest(httpRequest);
        log.debug("Getting labs for user ID: {}", userId);
        LabListResponse response = labService.getMyLabs(userId, page, limit);
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
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation =
                            LabListResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required")
    })
    @RequireAuth
    @GetMapping
    public ResponseEntity<LabListResponse> getLabs(
            @ParameterObject @Valid LabsGetRequest request,
            HttpServletRequest httpRequest) {
        log.debug("Received request to get labs with page: {}, limit: {}", request.getPage(), request.getLimit());
        LabListResponse response = labService.getLabs(request);
        log.debug("Successfully retrieved labs list with {} labs", response.getLabs().size());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Delete lab",
            description = "Deletes a specific lab by its ID. Only the lab owner can delete it. Requires authentication."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Lab deleted successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation =
                            LabDeleteResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - No access to delete the lab"),
            @ApiResponse(responseCode = "404", description = "Lab not found")
    })
    @RequireAuth
    @DeleteMapping("/{lab_id}")
    public ResponseEntity<LabDeleteResponse> deleteLab(
            @Parameter(description = "ID of the lab to delete", required = true)
            @PathVariable("lab_id") Long labId,
            HttpServletRequest request) {
        log.debug("Received request to delete lab with ID: {}", labId);
        Long userId = attributesProvider.extractUserIdFromRequest(request);
        LabDeleteResponse response = labService.deleteLab(labId, userId);
        log.debug("Successfully deleted lab with ID: {}", labId);
        return ResponseEntity.ok(response);
    }

    @RequireAuth
    @GetMapping("/{lab_id}/assets")
    public ResponseEntity<AssetListResponse> getLabAssets(
            @PathVariable("lab_id") Long labId,
            HttpServletRequest request) {
        log.debug("Received request to get assets for lab ID: {}", labId);
        try {
            AssetListResponse assets = labService.getLabAssets(labId);
            log.debug("Successfully retrieved {} assets for lab ID: {}", assets.getTotalCount(), labId);
            return ResponseEntity.ok(assets);
        } catch (Exception e) {
            log.error("Failed to get assets for lab ID: {}", labId, e);
            return ResponseEntity.status(500).body(null);
        }
    }

    @RequireAuth
    @GetMapping("/{lab_id}/assets/{asset_id}/download")
    public ResponseEntity<?> downloadLabAsset(
            @PathVariable("lab_id") Long labId,
            @PathVariable("asset_id") Long assetId,
            HttpServletRequest request) {
        log.debug("Received request to download asset ID: {} for lab ID: {}", assetId, labId);
        try {
            byte[] content = labService.downloadLabAsset(assetId);
            log.debug("Successfully downloaded asset ID: {}, size: {} bytes", assetId, content.length);

            return ResponseEntity.ok()
                    .header("Content-Type", "application/octet-stream")
                    .header("Content-Disposition", "attachment")
                    .body(content);
        } catch (Exception e) {
            log.error("Failed to download asset ID: {} for lab ID: {}", assetId, labId, e);
            return ResponseEntity.status(500).body("Failed to download asset: " + e.getMessage());
        }
    }

}

