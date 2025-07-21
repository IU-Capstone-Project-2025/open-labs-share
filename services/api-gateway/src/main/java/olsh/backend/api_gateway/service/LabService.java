package olsh.backend.api_gateway.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import olsh.backend.api_gateway.config.UploadFileConfiguration;
import olsh.backend.api_gateway.dto.request.LabCreateRequest;
import olsh.backend.api_gateway.dto.request.LabsGetRequest;
import olsh.backend.api_gateway.dto.response.*;
import olsh.backend.api_gateway.exception.ForbiddenAccessException;
import olsh.backend.api_gateway.exception.LabNotFoundException;
import olsh.backend.api_gateway.grpc.client.LabServiceClient;
import olsh.backend.api_gateway.grpc.proto.LabProto;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LabService {

    private final LabServiceClient labServiceClient;
    private final UploadFileConfiguration uploadConfig;
    private final UserService userService;
    private final TagService tagService;

    /**
     * Creates a new lab with the provided request and owner ID.
     *
     * @param request  the LabCreateRequest containing lab details
     * @param ownerId  the ID of the user creating the lab
     * @return LabCreateResponse containing the created lab ID and success message
     */
    public LabCreateResponse createLab(LabCreateRequest request, Long ownerId) {
        log.debug("Creating lab with title: {} for owner: {}", request.getTitle(), ownerId);
        validateMarkdownFile(request.getMd_file());
        List<MultipartFile> assets = validateAssets(request.getAssets());
        LabProto.Lab lab = registerLab(request, ownerId);
        labServiceClient.uploadAsset(lab.getLabId(), request.getMd_file());
        for (MultipartFile asset : assets) {
            if (asset != null && !asset.isEmpty()) {
                labServiceClient.uploadAsset(lab.getLabId(), asset);
            }
        }
        log.info("Lab created successfully with ID: {}", lab.getLabId());
        return LabCreateResponse.builder()
                .id(lab.getLabId())
                .message("Lab created successfully!")
                .build();
    }

    /**
     * Validates the provided Markdown file for lab creation.
     *
     * @param file the MultipartFile representing the Markdown file
     * @throws IllegalArgumentException if the file is invalid
     */
    protected void validateMarkdownFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Markdown file is required");
        }

        if (file.getOriginalFilename() == null || file.getOriginalFilename().isBlank() ||
                !file.getOriginalFilename().toLowerCase().endsWith(".md")) {
            throw new IllegalArgumentException("Only Markdown files are allowed");
        }

        if (file.getSize() > uploadConfig.getMaxFileSize()) {
            throw new IllegalArgumentException(String.format("File size exceeds maximum limit of %d bytes",
                    uploadConfig.getMaxFileSize()));
        }
    }

    /**
     * Validates the provided assets for lab creation.
     *
     * @param assets the array of MultipartFile representing the assets
     * @return a list of valid MultipartFile assets
     */
    protected List<MultipartFile> validateAssets(MultipartFile[] assets) {
        if (assets == null) {
            return Collections.emptyList();
        }
        if (assets.length == 0 || (assets.length == 1 && assets[0].isEmpty())) {
            return Collections.emptyList();
        }
        List<MultipartFile> validAssets = new ArrayList<>();
        for (MultipartFile asset : assets) {
            asset = validateAsset(asset);
            if (asset != null) {
                validAssets.add(asset);
            }
        }
        return validAssets;
    }

    /**
     * Validates a single asset file for lab creation.
     *
     * @param asset the MultipartFile representing the asset
     * @return the validated MultipartFile if valid, null otherwise
     * @throws IllegalArgumentException if the asset is invalid
     */
    protected MultipartFile validateAsset(MultipartFile asset) {
        if (asset == null || asset.isEmpty()) {
            return null;
        }
        if (asset.getOriginalFilename() == null || asset.getOriginalFilename().isBlank()) {
            throw new IllegalArgumentException("Asset name cannot be empty.");
        }
        if (asset.getOriginalFilename().endsWith(".md")) {
            throw new IllegalArgumentException("Asset for lab cannot contain an .md file.");
        }
        if (asset.getSize() > uploadConfig.getMaxFileSize()) {
            throw new IllegalArgumentException(String.format("Asset file size for lab exceeds maximum limit of %d " +
                            "bytes",
                    uploadConfig.getMaxFileSize()));
        }
        return asset;
    }

    /**
     * Registers a new lab with the provided request and owner ID.
     *
     * @param request  the LabCreateRequest containing lab details
     * @param ownerId  the ID of the user creating the lab
     * @return LabProto.Lab containing the created lab details
     */
    private LabProto.Lab registerLab(LabCreateRequest request, Long ownerId) {
        log.debug("Registering lab with title: {} for owner: {}", request.getTitle(), ownerId);
        LabProto.CreateLabRequest.Builder builder =
                LabProto.CreateLabRequest.newBuilder()
                        .setOwnerId(ownerId)
                        .setTitle(request.getTitle())
                        .setAbstract(request.getShort_desc());
        List<Long> articles = request.getArticlesList();
        builder.addAllRelatedArticlesIds(articles);
        List<Integer> tags = request.getTagsList();
        builder.addAllTagsIds(tags);
        LabProto.CreateLabRequest grpcRequest = builder.build();
        LabProto.Lab lab = labServiceClient.createLab(grpcRequest);
        log.debug("Successfully registered lab with ID: {}", lab.getLabId());
        return lab;
    }

    /**
     * Retrieves a lab by its ID, including its author and assets.
     *
     * @param labId the ID of the lab to retrieve
     * @return LabAndTagsResponse containing the lab details, author, assets, and tags
     * @throws IllegalArgumentException if labId is null or invalid
     */
    public LabAndTagsResponse getLabById(Long labId) {
        if (labId == null || labId <= 0) {
            throw new IllegalArgumentException("LabId should be provided");
        }
        log.debug("Getting lab with ID: {}", labId);
        LabProto.Lab lab = labServiceClient.getLab(labId);
        UserResponse author = userService.getUserById(lab.getOwnerId());
        LabProto.AssetList assets = labServiceClient.listAssets(labId);
        List<TagResponse> tags = tagService.getTagsByIds(lab.getTagsIdsList()).getTags();
        LabAndTagsResponse response = buildLabAndTagsResponse(lab, author, assets, tags);
        log.debug("Successfully retrieved lab: {}", lab.getTitle());
        return response;
    }

    /**
     * Retrieves a list of labs based on the provided request parameters.
     *
     * @param request the LabsGetRequest containing pagination and filtering options
     * @return LabListResponse containing the list of labs, total count, and associated tags
     */
    public LabListResponse getLabs(LabsGetRequest request) {
        log.debug("Getting labs list - page: {}, limit: {}", request.getPage(), request.getLimit());
        LabProto.GetLabsRequest.Builder grpcRequest = LabProto.GetLabsRequest.newBuilder()
                .setPageNumber(request.getPage())
                .setPageSize(request.getLimit())
                .addAllTagsIds(request.getTagsList());
        if (!request.getText().isBlank()) {
            grpcRequest.setText(request.getText());
        }
        LabProto.LabList grpcResponse = labServiceClient.getLabs(grpcRequest.build());
        LabListResponse response = buildLabListResponseFromProto(grpcResponse.getLabsList());
        log.info("Successfully retrieved {} labs out of {} total", response.getLabs().size(),
                grpcResponse.getLabsCount());
        return response;
    }

    /**
     * Retrieves a list of labs created by a specific user.
     *
     * @param id    the ID of the user whose labs are to be retrieved
     * @param page  the page number for pagination
     * @param limit the number of labs per page
     * @return LabListResponse containing the user's labs, total count, and associated tags
     */
    public LabListResponse getMyLabs(Long id, Integer page, Integer limit) {
        log.debug("Getting articles for author {} - page: {}, limit: {}", id, page, limit);
        LabProto.GetLabsByUserIdRequest request = LabProto.GetLabsByUserIdRequest.newBuilder()
                .setUserId(id)
                .setPageNumber(page)
                .setPageSize(limit)
                .build();
        LabProto.LabList grpcResponse = labServiceClient.getUsersLabs(request);
        LabListResponse response = buildLabListResponseFromProto(grpcResponse.getLabsList());
        log.info("Successfully retrieved {} user's (ID: {}) labs out of {} total", response.getLabs().size(), id,
                grpcResponse.getLabsCount());
        return response;
    }

    /**
     * Deletes a lab by its ID, ensuring the user has permission to delete it.
     *
     * @param labId  the ID of the lab to delete
     * @param userId the ID of the user attempting to delete the lab
     * @return LabDeleteResponse containing a success message
     * @throws ForbiddenAccessException if the user does not own the lab
     */
    public LabDeleteResponse deleteLab(Long labId, Long userId) {
        log.debug("Deleting lab with ID: {} by user: {}", labId, userId);

        LabProto.Lab lab = labServiceClient.getLab(labId);
        if (lab.getOwnerId() != userId.longValue()) {
            throw new ForbiddenAccessException("You can't delete a lab that you don't own!");
        }

        boolean success = labServiceClient.deleteLab(labId);
        if (!success) {
            throw new RuntimeException("Failed to delete lab");
        }

        return LabDeleteResponse.builder()
                .message("Lab deleted successfully!")
                .build();
    }

    /**
     * Validates the existence of a lab by its ID.
     *
     * @param id the ID of the lab to validate
     * @throws LabNotFoundException if the lab does not exist
     */
    protected void validateLabExists(Long id) throws LabNotFoundException {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("LabId should be provided");
        }
        log.debug("Validating existence of lab with ID: {}", id);
        LabProto.Lab lab = labServiceClient.getLab(id);
    }

    /**
     * Validates if the user is the author of the lab.
     *
     * @param labId  the ID of the lab
     * @param userId the ID of the user to check
     * @return true if the user is the author, false otherwise
     */
    protected boolean validateLabAuthorId(Long labId, Long userId) {
        log.debug("Checking if user {} is the author of lab with ID: {}", userId, labId);
        LabProto.Lab lab = labServiceClient.getLab(labId);
        return lab.getOwnerId() == userId;
    }

    /**
     * Retrieves a list of assets associated with a specific lab.
     *
     * @param labId the ID of the lab for which to retrieve assets
     * @return AssetListResponse containing the list of assets and total count
     */
    public AssetListResponse getLabAssets(Long labId) {
        log.debug("Getting assets for lab ID: {}", labId);
        LabProto.AssetList assetList = labServiceClient.listAssets(labId);

        // Convert protobuf objects to DTOs
        List<AssetResponse> assetResponses = assetList.getAssetsList().stream()
                .map(this::mapAssetToResponse)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

        return AssetListResponse.builder()
                .totalCount(assetList.getTotalCount())
                .assets(assetResponses)
                .build();
    }

    /**
     * Downloads a specific asset by its ID.
     *
     * @param assetId the ID of the asset to download
     * @return byte array containing the asset data
     */
    public byte[] downloadLabAsset(Long assetId) {
        log.debug("Downloading asset with ID: {}", assetId);
        return labServiceClient.downloadAsset(assetId);
    }

    /**
     * Maps a LabProto.Asset to AssetResponse.
     *
     * @param asset the LabProto.Asset to map
     * @return AssetResponse containing the mapped fields
     */
    private AssetResponse mapAssetToResponse(LabProto.Asset asset) {
        return AssetResponse.builder()
                .assetId(asset.getAssetId())
                .labId(asset.getLabId())
                .filename(asset.getFilename())
                .totalSize(asset.getFilesize())
                .uploadDate(TimestampConverter.convertTimestampToIso(asset.getUploadDate()))
                .build();
    }

    /**
     * Builds a list of AssetResponse from LabProto.AssetList.
     *
     * @param assets the LabProto.AssetList containing assets
     * @return List of AssetResponse mapped from the assets
     */
    private List<AssetResponse> buildAssetResponse(LabProto.AssetList assets) {
        return assets.getAssetsList().stream()
                .map(this::mapAssetToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Builds a LabResponse from Lab proto and UserResponse
     *
     * @param lab    the Lab proto object
     * @param author the UserResponse containing author information
     * @return LabResponse with all fields mapped
     */
    private LabResponse buildLabResponse(LabProto.Lab lab, UserResponse author, LabProto.AssetList assets) {
        return LabResponse.builder()
                .id(lab.getLabId())
                .title(lab.getTitle())
                .shortDesc(lab.getAbstract())
                .createdAt(TimestampConverter.convertTimestampToIso(lab.getCreatedAt()))
                .views(lab.getViews())
                .submissions(lab.getSubmissions())
                .authorId(lab.getOwnerId())
                .authorName(author.getName())
                .authorSurname(author.getSurname())
                .assets(buildAssetResponse(assets))
                .articles(lab.getRelatedArticlesIdsList())
                .tags(lab.getTagsIdsList())
                .build();
    }

    /**
     * Builds a LabResponse from Lab proto and UserResponse
     *
     * @param lab    the Lab proto object
     * @param author the UserResponse containing author information
     * @param assets the AssetList containing assets for the lab
     * @param tags   the list of TagResponse objects associated with the lab
     * @return LabResponse with all fields mapped
     */
    private LabAndTagsResponse buildLabAndTagsResponse(LabProto.Lab lab, UserResponse author, LabProto.AssetList assets,
                                                       List<TagResponse> tags) {
        return LabAndTagsResponse.builder()
                .id(lab.getLabId())
                .title(lab.getTitle())
                .shortDesc(lab.getAbstract())
                .createdAt(TimestampConverter.convertTimestampToIso(lab.getCreatedAt()))
                .views(lab.getViews())
                .submissions(lab.getSubmissions())
                .authorId(lab.getOwnerId())
                .authorName(author.getName())
                .authorSurname(author.getSurname())
                .assets(buildAssetResponse(assets))
                .articles(lab.getRelatedArticlesIdsList())
                .tags(tags)
                .build();
    }

    /**
     * Builds a LabListResponse from a list of LabProto.Lab objects.
     *
     * @param labs the list of LabProto.Lab objects
     * @return LabListResponse containing the list of labs and associated tags
     */
    private LabListResponse buildLabListResponseFromProto(List<LabProto.Lab> labs){
        List<LabResponse> labResponses = new ArrayList<>();
        HashMap<Long, UserResponse> authorCache = new HashMap<>();
        for (LabProto.Lab lab : labs) {
            try {
                UserResponse author = authorCache.computeIfAbsent(lab.getOwnerId(), userService::getUserById);
                LabProto.AssetList assets = labServiceClient.listAssets(lab.getLabId());
                labResponses.add(buildLabResponse(lab, author, assets));
            } catch (Exception e) {
                log.warn("Skipping lab with ID {} due to an error fetching its owner (owner_id={}): {}",
                        lab.getLabId(), lab.getOwnerId(), e.getMessage());
            }
        }
        return buildLabListResponse(labResponses);
    }

    /**
     * Builds a LabListResponse from a list of LabResponse objects.
     *
     * @param labResponses the list of LabResponse objects
     * @return LabListResponse containing the list of labs and associated tags
     */
    private LabListResponse buildLabListResponse(List<LabResponse> labResponses) {
        List<Integer> tagsIdsList = labResponses.stream()
                .flatMap(labResponse -> labResponse.getTags().stream())
                .distinct()
                .collect(Collectors.toList());
        List<TagResponse> tags = tagService.getTagsByIds(tagsIdsList).getTags();
        return LabListResponse.builder()
                .labs(labResponses)
                .count(labResponses.size())
                .tags(tags)
                .build();
    }

}

