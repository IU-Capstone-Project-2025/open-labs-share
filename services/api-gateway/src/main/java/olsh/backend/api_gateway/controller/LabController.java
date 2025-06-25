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

@Slf4j
@RestController
@RequestMapping("/api/v1/labs")
@CrossOrigin(origins = "*", maxAge = 3600)
public class LabController {

    private final LabService labService;
    private final RequestAttributesExtractor attributesProvider;


    @Autowired
    public LabController(LabService labService, RequestAttributesExtractor attributesProvider) {
        this.labService = labService;
        this.attributesProvider  = attributesProvider;
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
    public ResponseEntity<LabListResponse> getLabs(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "limit", defaultValue = "20") Integer limit,
            HttpServletRequest httpRequest) {
        log.debug("Received request to get labs with page: {}, limit: {}", page, limit);
        
        // Validate parameters manually
        if (page < 1) {
            page = 1;
        }
        if (limit < 1) {
            limit = 20;
        }
        if (limit > 100) {
            limit = 100;
        }
        
        // Create the request object manually
        GetLabsRequest request = new GetLabsRequest();
        request.setPage(page);
        request.setLimit(limit);
        
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

}

