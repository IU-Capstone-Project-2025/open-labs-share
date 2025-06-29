package olsh.backend.api_gateway.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import olsh.backend.api_gateway.config.UploadFileConfiguration;
import olsh.backend.api_gateway.dto.request.CreateLabRequest;
import olsh.backend.api_gateway.dto.request.GetLabsRequest;
import olsh.backend.api_gateway.dto.response.*;
import olsh.backend.api_gateway.exception.ForbiddenAccessException;
import olsh.backend.api_gateway.grpc.client.LabServiceClient;
import olsh.backend.api_gateway.grpc.proto.LabProto;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LabService {

    private final LabServiceClient labServiceClient;
    private final UploadFileConfiguration uploadConfig;
    private final UserService userService;

    public CreateLabResponse createLab(CreateLabRequest request, Long ownerId) {
        log.debug("Creating lab with title: {} for owner: {}", request.getTitle(), ownerId);

        validateMarkdownFile(request.getMd_file());
        validateAssets(request.getAssets());

        LabProto.Lab lab = registerLab(request, ownerId);

        // Upload main markdown file
        labServiceClient.uploadAsset(lab.getLabId(), request.getMd_file());

        // Upload additional assets if provided
        if (request.getAssets() != null) {
            for (MultipartFile asset : request.getAssets()) {
                if (asset != null && !asset.isEmpty()) {
                    labServiceClient.uploadAsset(lab.getLabId(), asset);
                }
            }
        }

        return CreateLabResponse.builder()
                .id(lab.getLabId())
                .message("Lab created successfully!")
                .build();
    }

    private void validateMarkdownFile(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getOriginalFilename() == null) {
            throw new IllegalArgumentException("Markdown file is required");
        }

        if (!file.getOriginalFilename().toLowerCase().endsWith(".md")) {
            throw new IllegalArgumentException("Only Markdown files are allowed");
        }

        if (file.getSize() > uploadConfig.getMaxFileSize()) {
            throw new IllegalArgumentException(String.format("File size exceeds maximum limit of %d bytes",
                    uploadConfig.getMaxFileSize()));
        }
    }

    private void validateAssets(MultipartFile[] assets) {
        if (assets == null) {
            return;
        }
        for (MultipartFile asset : assets) {
            validateAsset(asset);
        }
    }

    private void validateAsset(MultipartFile asset){
        if (asset == null || asset.isEmpty()) {
            throw new IllegalArgumentException("Asset file for lab is empty or null arrived");
        }
        if (asset.getOriginalFilename() == null || asset.getOriginalFilename().isBlank()) {
            throw new IllegalArgumentException("Asset name cannot be empty.");
        }
        if (asset.getOriginalFilename().endsWith(".md")) {
            throw new IllegalArgumentException("Asset for lab cannot contain an .md file.");
        }
        if (asset.getSize() > uploadConfig.getMaxFileSize()) {
            throw new IllegalArgumentException(String.format("Asset file size for lab exceeds maximum limit of %d bytes",
                    uploadConfig.getMaxFileSize()));
        }
    }

    private LabProto.Lab registerLab(CreateLabRequest request, Long ownerId) {
        LabProto.CreateLabRequest grpcRequest =
                LabProto.CreateLabRequest.newBuilder()
                        .setOwnerId(ownerId)
                        .setTitle(request.getTitle())
                        .setAbstract(request.getShort_desc())
                        .build();

        LabProto.Lab lab = labServiceClient.createLab(grpcRequest);
        log.debug("Successfully registered lab with ID: {}", lab.getLabId());
        return lab;
    }

    public LabResponse getLabById(Long labId) {
        if (labId == null || labId == 0) {
            throw new IllegalArgumentException("LabId should be provided");
        }

        log.debug("Getting lab with ID: {}", labId);
        LabProto.Lab lab = labServiceClient.getLab(labId);
        UserResponse author = userService.getUserById(lab.getOwnerId());

        log.debug("Successfully retrieved lab: {}", lab.getTitle());
        return buildLabResponse(lab, author);
    }

    public LabListResponse getLabs(GetLabsRequest request) {
        log.debug("Getting labs list - page: {}, limit: {}", request.getPage(), request.getLimit());

        try {
            LabProto.LabList grpcResponse = labServiceClient.getLabs(request.getPage(), request.getLimit());

            List<LabResponse> labResponses = new ArrayList<>();
            HashMap<Long, UserResponse> authorCache = new HashMap<>();

            for (LabProto.Lab lab : grpcResponse.getLabsList()) {
                try {
                    UserResponse author;
                    if (authorCache.containsKey(lab.getOwnerId())) {
                        author = authorCache.get(lab.getOwnerId());
                    } else {
                        author = userService.getUserById(lab.getOwnerId());
                        authorCache.put(lab.getOwnerId(), author);
                    }
                    labResponses.add(buildLabResponse(lab, author));
                } catch (Exception e) {
                    log.warn("Skipping lab with ID {} due to an error fetching its owner (owner_id={}): {}", 
                             lab.getLabId(), lab.getOwnerId(), e.getMessage());
                }
            }

            int totalItems = (int) grpcResponse.getTotalCount();
            int totalPages = (int) Math.ceil((double) totalItems / request.getLimit());

            LabListResponse.PaginationResponse pagination =
                    LabListResponse.PaginationResponse.builder()
                            .currentPage(request.getPage())
                            .totalPages(totalPages)
                            .totalItems(totalItems)
                            .build();

            log.debug("Successfully retrieved {} labs out of {} total", labResponses.size(), totalItems);

            return LabListResponse.builder()
                    .labs(labResponses)
                    .pagination(pagination)
                    .build();

        } catch (Exception e) {
            log.error("Error getting labs list: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve labs: " + e.getMessage());
        }
    }

    public LabListResponse getMyLabs(GetLabsRequest request, Long userId){
        LabListResponse allLabsResponse = getLabs(request);

        // Filter labs by current user
        List<LabResponse> userLabs = allLabsResponse.getLabs().stream()
                .filter(lab -> lab.getAuthorId().equals(userId))
                .collect(Collectors.toList());

        // Build filtered response
        LabListResponse.PaginationResponse pagination =
                LabListResponse.PaginationResponse.builder()
                        .currentPage(request.getPage())
                        .totalPages(1)
                        .totalItems(userLabs.size())
                        .build();

        LabListResponse response = LabListResponse.builder()
                .labs(userLabs)
                .pagination(pagination)
                .build();

        log.debug("Successfully retrieved my labs list with {} labs for user {}",
                userLabs.size(), userId);
        return response;
    }

    public DeleteLabResponse deleteLab(Long labId, Long userId) {
        log.debug("Deleting lab with ID: {} by user: {}", labId, userId);

        LabProto.Lab lab = labServiceClient.getLab(labId);
        if (lab.getOwnerId() != userId.longValue()) {
            throw new ForbiddenAccessException("You can't delete a lab that you don't own!");
        }

        boolean success = labServiceClient.deleteLab(labId);
        if (!success) {
            throw new RuntimeException("Failed to delete lab");
        }

        return DeleteLabResponse.builder()
                .message("Lab deleted successfully!")
                .build();
    }

    // New methods for asset management
    public AssetListResponse getLabAssets(Long labId) {
        log.debug("Getting assets for lab ID: {}", labId);
        LabProto.AssetList assetList = labServiceClient.listAssets(labId);
        
        // Convert protobuf objects to DTOs
        List<AssetResponse> assetResponses = assetList.getAssetsList().stream()
                .map(this::convertAssetToResponse)
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

    private AssetResponse convertAssetToResponse(LabProto.Asset asset) {
        return AssetResponse.builder()
                .assetId(asset.getAssetId())
                .labId(asset.getLabId())
                .filename(asset.getFilename())
                .totalSize(asset.getFilesize())
                .uploadDate(convertTimestampToIso(asset.getUploadDate()))
                .build();
    }

    private String convertTimestampToIso(com.google.protobuf.Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }

        try {
            java.time.Instant instant = java.time.Instant.ofEpochSecond(
                    timestamp.getSeconds(), timestamp.getNanos());
            return instant.toString();
        } catch (Exception e) {
            log.warn("Failed to convert timestamp: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Builds a LabResponse from Lab proto and UserResponse
     *
     * @param lab the Lab proto object
     * @param author the UserResponse containing author information
     * @return LabResponse with all fields mapped
     */
    private LabResponse buildLabResponse(LabProto.Lab lab, UserResponse author) {
        return LabResponse.builder()
                .id(lab.getLabId())
                .title(lab.getTitle())
                .shortDesc(lab.getAbstract())
                .createdAt(convertTimestampToIso(lab.getCreatedAt()))
                .views(lab.getViews())
                .submissions(lab.getSubmissions())
                .authorId(lab.getOwnerId())
                .authorName(author.name())
                .authorSurname(author.surname())
                .build();
    }

}

