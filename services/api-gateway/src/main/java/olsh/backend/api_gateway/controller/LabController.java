package olsh.backend.api_gateway.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import olsh.backend.api_gateway.annotation.RequireAuth;
import olsh.backend.api_gateway.dto.request.CreateLabRequest;
import olsh.backend.api_gateway.dto.request.GetLabsRequest;
import olsh.backend.api_gateway.dto.response.CreateLabResponse;
import olsh.backend.api_gateway.dto.response.DeleteLabResponse;
import olsh.backend.api_gateway.dto.response.LabListResponse;
import olsh.backend.api_gateway.dto.response.LabResponse;
import olsh.backend.api_gateway.dto.response.AssetListResponse;
import olsh.backend.api_gateway.service.LabService;
import olsh.backend.api_gateway.controller.RequestAttributesExtractor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/labs")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true", maxAge = 3600)
public class LabController {

    private final LabService labService;
    private final RequestAttributesExtractor attributesProvider;
    private final Validator validator;


    @Autowired
    public LabController(LabService labService, RequestAttributesExtractor attributesProvider, Validator validator) {
        this.labService = labService;
        this.attributesProvider  = attributesProvider;
        this.validator = validator;
    }

    @RequireAuth
    @PostMapping
    public ResponseEntity<CreateLabResponse> createLab(
            @Valid @ModelAttribute CreateLabRequest request,
            HttpServletRequest httpRequest) {
        log.debug("Received request to create lab with title: {}", request.getTitle());
        CreateLabResponse response = labService.createLab(request,
                attributesProvider.extractUserIdFromRequest(httpRequest));
        log.debug("Successfully created lab");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @RequireAuth
    @GetMapping("/{lab_id}")
    public ResponseEntity<LabResponse> getLab(
            @PathVariable("lab_id") Long labId,
            HttpServletRequest request) {
        log.debug("Received request to get lab with ID: {}", labId);
        LabResponse response = labService.getLabById(labId);
        log.debug("Successfully retrieved lab data for labId: {}", labId);
        return ResponseEntity.ok(response);
    }

    @RequireAuth
    @GetMapping
    public ResponseEntity<?> getLabs(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "limit", defaultValue = "20") Integer limit,
            HttpServletRequest httpRequest) {
        log.debug("Received request to get labs with page: {}, limit: {}", page, limit);

        GetLabsRequest request = new GetLabsRequest();
        request.setPage(page);
        request.setLimit(limit);

        Set<ConstraintViolation<GetLabsRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            String errors = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining(", "));
            return ResponseEntity.badRequest().body("Invalid arguments provided: " + errors);
        }
        
        LabListResponse response = labService.getLabs(request);
        log.debug("Successfully retrieved labs list with {} labs", response.getLabs().size());
        return ResponseEntity.ok(response);
    }

    @RequireAuth
    @DeleteMapping("/{lab_id}")
    public ResponseEntity<DeleteLabResponse> deleteLab(
            @PathVariable("lab_id") Long labId,
            HttpServletRequest request) {
        log.debug("Received request to delete lab with ID: {}", labId);
        Long userId = attributesProvider.extractUserIdFromRequest(request);
        DeleteLabResponse response = labService.deleteLab(labId, userId);
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

