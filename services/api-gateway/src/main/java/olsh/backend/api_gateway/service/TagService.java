package olsh.backend.api_gateway.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import olsh.backend.api_gateway.dto.request.TagCreateRequest;
import olsh.backend.api_gateway.dto.request.TagUpdateRequest;
import olsh.backend.api_gateway.dto.response.TagListResponse;
import olsh.backend.api_gateway.dto.response.TagResponse;
import olsh.backend.api_gateway.grpc.client.TagServiceClient;
import olsh.backend.api_gateway.grpc.proto.TagProto;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TagService {

    private final TagServiceClient tagServiceClient;

    /**
     * Creates a new tag with the provided name and description.
     *
     * @param request Contains tag name and description
     * @return Created tag response with all details
     */
    public TagResponse createTag(TagCreateRequest request) {
        log.debug("Creating tag with name: {}", request.getName());
        TagProto.Tag createdTag = tagServiceClient.createTag(request.getName(), request.getDescription());
        return mapToTagResponse(createdTag);
    }

    /**
     * Retrieves a specific tag by its ID.
     *
     * @param tagId The ID of the tag to retrieve
     * @return Tag response with all details
     */
    public TagResponse getTagById(long tagId) {
        log.debug("Getting tag by ID: {}", tagId);
        TagProto.Tag tag = tagServiceClient.getTag((int) tagId);
        return mapToTagResponse(tag);
    }

    /**
     * Retrieves multiple tags by their IDs.
     *
     * @param ids List of tag IDs to retrieve
     * @return List of tag responses
     */
    public TagListResponse getTagsByIds(List<Integer> ids) {
        log.debug("Getting tags by IDs: {}", ids);
        if (ids.isEmpty()){
            return TagListResponse.builder().tags(Collections.emptyList()).count(0).build();
        }
        TagProto.TagList tagList = tagServiceClient.getTagsByIds(ids);
        return mapToTagListResponse(tagList);
    }

    /**
     * Retrieves a paginated list of tags.
     *
     * @param page  Page number (1-based)
     * @param limit Number of tags per page
     * @return Paginated tag response
     */
    public TagListResponse getTags(int page, int limit) {
        log.debug("Getting tags with page: {}, limit: {}", page, limit);
        TagProto.TagList tagList = tagServiceClient.getTags(page, limit);
        return mapToTagListResponse(tagList);
    }

    /**
     * Updates an existing tag's name and/or description.
     *
     * @param request Contains optional name and description updates
     * @return Updated tag response
     */
    public TagResponse updateTag(TagUpdateRequest request) {
        log.debug("Updating tag ID: {} with request: {}", request.getId(), request);
        TagProto.UpdateTagRequest.Builder updateRequestBuilder = TagProto.UpdateTagRequest.newBuilder()
                .setId(request.getId().intValue());
        if (request.getName() != null) {
            updateRequestBuilder.setName(request.getName());
        }
        if (request.getDescription() != null) {
            updateRequestBuilder.setDescription(request.getDescription());
        }
        TagProto.UpdateTagRequest updateRequest = updateRequestBuilder.build();
        TagProto.Tag updatedTag = tagServiceClient.updateTag(updateRequest);
        return mapToTagResponse(updatedTag);
    }

    /**
     * Deletes a tag by its ID.
     *
     * @param tagId The ID of the tag to delete
     * @return true if deletion was successful, false otherwise
     */
    public boolean deleteTag(long tagId) {
        log.debug("Deleting tag with ID: {}", tagId);
        return tagServiceClient.deleteTag((int) tagId);
    }

    // ========== Private Helper Methods ==========

    /**
     * Maps a gRPC Tag proto message to a TagResponse DTO.
     *
     * @param tag The gRPC Tag proto message
     * @return TagResponse DTO
     */
    private TagResponse mapToTagResponse(TagProto.Tag tag) {
        return TagResponse.builder()
                .id(tag.getId())
                .name(tag.getName())
                .description(tag.getDescription())
                .labs_count(tag.getLabsCount())
                .build();
    }

    /**
     * Maps a gRPC TagList proto message to a TagListResponse DTO.
     *
     * @param tagList The gRPC TagList proto message
     * @return TagListResponse DTO
     */
    private TagListResponse mapToTagListResponse(TagProto.TagList tagList) {
        List<TagResponse> tagResponses = tagList.getTagsList().stream()
                .map(this::mapToTagResponse)
                .collect(Collectors.toList());
        return TagListResponse.builder()
                .tags(tagResponses)
                .count(tagList.getCount())
                .build();
    }
}