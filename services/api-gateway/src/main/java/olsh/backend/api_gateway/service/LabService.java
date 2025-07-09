package olsh.backend.api_gateway.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import olsh.backend.api_gateway.config.UploadFileConfiguration;
import olsh.backend.api_gateway.dto.request.LabCreateRequest;
import olsh.backend.api_gateway.dto.request.GetLabsRequest;
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

    private LabProto.Lab registerLab(LabCreateRequest request, Long ownerId) {
        log.debug("Registering lab with title: {} for owner: {}", request.getTitle(), ownerId);
        LabProto.CreateLabRequest.Builder builder =
                LabProto.CreateLabRequest.newBuilder()
                        .setOwnerId(ownerId)
                        .setTitle(request.getTitle())
                        .setAbstract(request.getShort_desc());
        List<Long> articles = request.getArticlesAsArray();
        builder.addAllRelatedArticlesIds(articles);
        List<Integer> tags = request.getTagsAsArray().stream().map(Long::intValue).toList();
        builder.addAllTagsIds(tags);
        LabProto.CreateLabRequest grpcRequest = builder.build();
        LabProto.Lab lab = labServiceClient.createLab(grpcRequest);
        log.debug("Successfully registered lab with ID: {}", lab.getLabId());
        return lab;
    }

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

    public LabListResponse getLabs(GetLabsRequest request) {
        log.debug("Getting labs list - page: {}, limit: {}", request.getPage(), request.getLimit());
        try {
            LabProto.LabList grpcResponse = labServiceClient.getLabs(request.getPage(), request.getLimit());
            List<LabResponse> labResponses = new ArrayList<>();
            HashMap<Long, UserResponse> authorCache = new HashMap<>();
            for (LabProto.Lab lab : grpcResponse.getLabsList()) {
                try {
                    UserResponse author = authorCache.computeIfAbsent(lab.getOwnerId(), userService::getUserById);
                    LabProto.AssetList assets = labServiceClient.listAssets(lab.getLabId());
                    labResponses.add(buildLabResponse(lab, author, assets));
                } catch (Exception e) {
                    log.warn("Skipping lab with ID {} due to an error fetching its owner (owner_id={}): {}",
                            lab.getLabId(), lab.getOwnerId(), e.getMessage());
                }
            }
            log.debug("Successfully retrieved {} labs out of {} total", labResponses.size(),
                    grpcResponse.getLabsCount());
            return buildLabListResponse(labResponses);
        } catch (Exception e) {
            log.error("Error getting labs list: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve labs: " + e.getMessage());
        }
    }

    public LabListResponse getMyLabs(GetLabsRequest request, Long userId) {
        LabListResponse allLabsResponse = getLabs(request);
        // Filter labs by current user
        // TODO: Tell Timur to add this functionality to gRPC service
        List<LabResponse> userLabs = allLabsResponse.getLabs().stream()
                .filter(lab -> lab.getAuthorId().equals(userId))
                .collect(Collectors.toList());
        LabListResponse response = buildLabListResponse(userLabs);
        log.debug("Successfully retrieved my labs list with {} labs for user {}",
                userLabs.size(), userId);
        return response;
    }

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

    protected void validateLabExists(Long labId) throws LabNotFoundException {
        log.debug("Validating existence of lab with ID: {}", labId);
        LabProto.Lab lab = labServiceClient.getLab(labId);
    }

    protected boolean validateLabAuthorId(Long labId, Long userId) {
        log.debug("Checking if user {} is the author of lab with ID: {}", userId, labId);
        LabProto.Lab lab = labServiceClient.getLab(labId);
        return lab.getOwnerId() == userId;
    }

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

    public byte[] downloadLabAsset(Long assetId) {
        log.debug("Downloading asset with ID: {}", assetId);
        return labServiceClient.downloadAsset(assetId);
    }

    private AssetResponse mapAssetToResponse(LabProto.Asset asset) {
        return AssetResponse.builder()
                .assetId(asset.getAssetId())
                .labId(asset.getLabId())
                .filename(asset.getFilename())
                .totalSize(asset.getFilesize())
                .uploadDate(TimestampConverter.convertTimestampToIso(asset.getUploadDate()))
                .build();
    }

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

