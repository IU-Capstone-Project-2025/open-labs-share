package olsh.backend.api_gateway.grpc.client;

import io.grpc.Channel;
import lombok.extern.slf4j.Slf4j;
import olsh.backend.api_gateway.exception.TagNotFoundException;
import olsh.backend.api_gateway.grpc.proto.TagProto.*;
import olsh.backend.api_gateway.grpc.proto.TagServiceGrpc;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Client for interacting with the Tag gRPC service.
 * Handles all tag-related operations including tag management.
 */
@Slf4j
@Service
public class TagServiceClient {

    private final TagServiceGrpc.TagServiceBlockingStub blockingStub;

    public TagServiceClient(GrpcChannelFactory channelFactory) {
        Channel channel = channelFactory.createChannel("lab-service");
        this.blockingStub = TagServiceGrpc.newBlockingStub(channel);
    }


    /**
     * Creates a new tag with the specified name and description.
     *
     * @param name        The name of the tag
     * @param description The description of the tag
     * @return The created tag with all details
     * @throws RuntimeException if the gRPC call fails
     */
    public Tag createTag(String name, String description) {
        log.debug("Calling tag-service gRPC CreateTag with name: {}", name);
        try {
            CreateTagRequest request = CreateTagRequest.newBuilder()
                    .setName(name)
                    .setDescription(description)
                    .build();
            Tag response = blockingStub.createTag(request);
            log.debug("Successfully created tag via gRPC with ID: {}", response.getId());
            return response;
        } catch (Exception e) {
            log.error("Error calling CreateTag gRPC: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create tag via gRPC", e);
        }
    }

    /**
     * Retrieves a specific tag by its ID.
     *
     * @param id The ID of the tag to retrieve
     * @return The tag if found
     * @throws TagNotFoundException if the tag doesn't exist
     * @throws RuntimeException if the gRPC call fails
     */
    public Tag getTag(int id) {
        log.debug("Calling gRPC GetTag for tag ID: {}", id);
        try {
            GetTagRequest request = GetTagRequest.newBuilder()
                    .setId(id)
                    .build();
            Tag response = blockingStub.getTag(request);
            log.debug("Successfully retrieved tag via gRPC with ID: {}", response.getId());
            return response;
        } catch (Exception e) {
            log.error("Error calling GetTag gRPC for ID {}: {}", id, e.getMessage(), e);
            if (e.getMessage().contains("NOT_FOUND")) {
                throw new TagNotFoundException(id);
            }
            throw new RuntimeException("Failed to get tag via gRPC", e);
        }
    }

    /**
     * Retrieves a paginated list of tags.
     *
     * @param pageNumber The page number to retrieve (1-based)
     * @param pageSize   The number of tags per page
     * @return Paginated list of tags with total count
     * @throws RuntimeException if the gRPC call fails
     */
    public TagList getTags(int pageNumber, int pageSize) {
        log.debug("Calling gRPC GetTags with page: {}, size: {}", pageNumber, pageSize);
        try {
            GetTagsRequest request = GetTagsRequest.newBuilder()
                    .setPageNumber(pageNumber)
                    .setPageSize(pageSize)
                    .build();
            TagList response = blockingStub.getTags(request);
            log.debug("Successfully retrieved {} tags via gRPC (total: {})",
                    response.getTagsCount(), response.getCount());
            return response;
        } catch (Exception e) {
            log.error("Error calling GetTags gRPC: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get tags via gRPC", e);
        }
    }

    /**
     * Retrieves tags by their IDs.
     *
     * @param ids List of tag IDs to retrieve
     * @return List of tags matching the provided IDs
     * @throws RuntimeException if the gRPC call fails
     */
    public TagList getTagsByIds(List<Integer> ids) {
        log.debug("Calling gRPC GetTagsByIds for {} IDs", ids.size());
        try {
            GetTagsByIdsRequest request = GetTagsByIdsRequest.newBuilder()
                    .addAllIds(ids)
                    .build();
            TagList response = blockingStub.getTagsByIds(request);
            log.debug("Successfully retrieved {} tags by IDs via gRPC", response.getTagsCount());
            return response;
        } catch (Exception e) {
            log.error("Error calling GetTagsByIds gRPC: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get tags by IDs via gRPC", e);
        }
    }

    /**
     * Updates an existing tag's name and/or description.
     * Uses the full request object since it has more than 2 parameters.
     *
     * @param request Contains tag ID and optional name and description updates
     * @return The updated tag with all details
     * @throws TagNotFoundException if the tag doesn't exist
     * @throws RuntimeException if the gRPC call fails
     */
    public Tag updateTag(UpdateTagRequest request) {
        log.debug("Calling gRPC UpdateTag for tag ID: {}", request.getId());
        try {
            Tag response = blockingStub.updateTag(request);
            log.debug("Successfully updated tag via gRPC with ID: {}", response.getId());
            return response;
        } catch (Exception e) {
            log.error("Error calling UpdateTag gRPC: {}", e.getMessage(), e);
            if (e.getMessage().contains("NOT_FOUND")) {
                throw new TagNotFoundException(String.format("Tag with id=%d not found", request.getId()));
            }
            throw new RuntimeException("Failed to update tag via gRPC", e);
        }
    }

    /**
     * Deletes a tag by its ID.
     *
     * @param id The ID of the tag to delete
     * @return true if deletion was successful, false otherwise
     * @throws TagNotFoundException if the tag doesn't exist
     * @throws RuntimeException if the gRPC call fails
     */
    public boolean deleteTag(int id) {
        log.debug("Calling gRPC DeleteTag for tag ID: {}", id);
        try {
            DeleteTagRequest request = DeleteTagRequest.newBuilder()
                    .setId(id)
                    .build();
            boolean success = blockingStub.deleteTag(request).getSuccess();
            log.debug("DeleteTag gRPC call completed with success: {}", success);
            return success;
        } catch (Exception e) {
            log.error("Error calling DeleteTag gRPC for ID {}: {}", id, e.getMessage(), e);
            if (e.getMessage().contains("NOT_FOUND")) {
                throw new TagNotFoundException(id);
            }
            throw new RuntimeException("Failed to delete tag via gRPC", e);
        }
    }
}
