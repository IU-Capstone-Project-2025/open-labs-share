package olsh.backend.api_gateway.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import olsh.backend.api_gateway.annotation.RequireAuth;
import olsh.backend.api_gateway.dto.request.TagCreateRequest;
import olsh.backend.api_gateway.dto.request.TagUpdateRequest;
import olsh.backend.api_gateway.dto.request.TagsGetByIdsRequest;
import olsh.backend.api_gateway.dto.response.TagListResponse;
import olsh.backend.api_gateway.dto.response.TagResponse;
import olsh.backend.api_gateway.service.TagService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tags")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "https://open-labs-share.online"}, allowCredentials = "true", maxAge = 3600)
@Tag(name = "Tags", description = "Endpoints for managing tags")
public class TagController {

    private final TagService tagService;

    @RequireAuth
    @PostMapping
    @Operation(summary = "Create a new tag", description = "Creates a new tag.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Tag created successfully", content = @Content(schema =
            @Schema(implementation = TagResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    public ResponseEntity<TagResponse> createTag(@Valid @RequestBody TagCreateRequest request) {
        TagResponse response = tagService.createTag(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{tagId}")
    @Operation(summary = "Get tag by ID", description = "Retrieves a tag by its ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tag found", content = @Content(schema =
            @Schema(implementation = TagResponse.class))),
            @ApiResponse(responseCode = "404", description = "Tag not found")
    })
    public ResponseEntity<TagResponse> getTag(
            @Parameter(description = "ID of the tag", required = true) @PathVariable long tagId) {
        TagResponse response = tagService.getTagById(tagId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/by-ids")
    @Operation(summary = "Get tags by IDs", description = "Retrieves a list of tags by their IDs.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tags found", content = @Content(schema =
            @Schema(implementation = TagListResponse.class)))
    })
    public ResponseEntity<TagListResponse> getTagsByIds(@Valid @RequestBody TagsGetByIdsRequest request) {
        TagListResponse response = tagService.getTagsByIds(request.getIds());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Get paginated list of tags", description = "Retrieves a paginated list of tags.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tags retrieved", content = @Content(schema =
            @Schema(implementation = TagListResponse.class)))
    })
    public ResponseEntity<TagListResponse> getTags(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "limit", defaultValue = "50") int limit
    ) {
        TagListResponse response = tagService.getTags(page, limit);
        return ResponseEntity.ok(response);
    }

    @RequireAuth
    @PutMapping("/update")
    @Operation(summary = "Update a tag", description = "Updates an existing tag.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tag updated", content = @Content(schema =
            @Schema(implementation = TagResponse.class))),
            @ApiResponse(responseCode = "404", description = "Tag not found")
    })
    public ResponseEntity<TagResponse> updateTag(
            @Valid @RequestBody TagUpdateRequest request) {
        TagResponse response = tagService.updateTag(request);
        return ResponseEntity.ok(response);
    }

    @RequireAuth
    @DeleteMapping("/{tagId}")
    @Operation(summary = "Delete a tag", description = "Deletes a tag by its ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Tag deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Tag not found")
    })
    public ResponseEntity<Boolean> deleteTag(@Parameter(description = "ID of the tag to delete", required = true) @PathVariable long tagId) {
        Boolean success = tagService.deleteTag(tagId);
        return ResponseEntity.ok(success);
    }
}
