package olsh.backend.api_gateway.service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import olsh.backend.api_gateway.config.UploadFileConfiguration;
import olsh.backend.api_gateway.dto.request.CreateSubmissionRequest;
import olsh.backend.api_gateway.dto.response.*;
import olsh.backend.api_gateway.exception.ForbiddenAccessException;
import olsh.backend.api_gateway.exception.SubmissionIsAlreadyGradedException;
import olsh.backend.api_gateway.grpc.client.SubmissionServiceClient;
import olsh.backend.api_gateway.grpc.proto.SubmissionProto;
import org.apache.catalina.User;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final SubmissionServiceClient submissionServiceClient;
    private final UploadFileConfiguration uploadConfig;
    private final UserService userService;
    private final LabService labService;

    public CreateSubmissionResponse createSubmission(CreateSubmissionRequest request, Long ownerId) {
        log.debug("Creating submission for lab ID: {} by owner: {}", request.getLabId(), ownerId);
        validateSubmissionFiles(request.getFiles());
        labService.validateLabExists(request.getLabId());
        if (labService.validateLabAuthorId(request.getLabId(), ownerId)){
            throw new ForbiddenAccessException("You cannot submit to your own lab");
        }
        SubmissionResponse submission = registerSubmission(request, ownerId);
        List<SubmissionAssetResponse> assets = uploadAssetsForSubmission(
                submission.getSubmissionId(),
                request.getFiles()
        );
        submission.setAssets(assets);
        // Increment labs solved for the user
        UserResponse user = userService.incrementLabsSolved(ownerId);
        submission.setOwner(user);
        return CreateSubmissionResponse.builder()
                .success(true)
                .message("Submission created successfully")
                .submissionMetadata(submission)
                .build();
    }

    private SubmissionResponse registerSubmission(CreateSubmissionRequest request, Long ownerId) {
        SubmissionProto.CreateSubmissionRequest protoRequest = SubmissionProto.CreateSubmissionRequest.newBuilder()
                .setLabId(request.getLabId())
                .setOwnerId(ownerId)
                .setText(request.getTextComment() != null ? request.getTextComment() : "")
                .build();
        log.debug("Registering submission with lab ID: {} and owner ID: {}", request.getLabId(), ownerId);
        SubmissionProto.Submission submission = submissionServiceClient.createSubmission(protoRequest);
        log.debug("Successfully registered submission with ID: {}", submission.getSubmissionId());
        UserResponse owner = userService.getUserByIdSafe(ownerId);
        return convertSubmissionToResponse(submission, owner, new ArrayList<>());
    }

    private List<SubmissionAssetResponse> uploadAssetsForSubmission(Long submissionId, MultipartFile[] files) {
        List<SubmissionAssetResponse> assetResponses = new ArrayList<>();
        log.debug("Uploading assets for submission ID: {}", submissionId);
        if (files != null) {
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    SubmissionProto.Asset asset = submissionServiceClient.uploadAsset(submissionId, file);
                    assetResponses.add(convertAssetToResponse(asset));
                }
            }
        }
        log.debug("Successfully uploaded {} assets for submission ID: {}", assetResponses.size(), submissionId);
        return assetResponses;
    }

    public SubmissionResponse getSubmissionById(Long submissionId) {
        if (submissionId == null || submissionId <= 0) {
            throw new IllegalArgumentException("Submission ID should be provided and positive");
        }

        log.debug("Getting submission with ID: {}", submissionId);

        SubmissionProto.Submission submission = submissionServiceClient.getSubmission(submissionId);
        UserResponse owner = userService.getUserByIdSafe(submission.getOwnerId());

        SubmissionProto.AssetList assetList = submissionServiceClient.listAssets(submissionId);
        List<SubmissionAssetResponse> assets = assetList.getAssetsList().stream()
                .map(this::convertAssetToResponse)
                .toList();

        return convertSubmissionToResponse(submission, owner, assets);
    }

    public SubmissionListResponse getSubmissionsByLabId(Long labId, Integer pageNum, Integer pageSize) {
        if (labId == null || labId <= 0) {
            throw new IllegalArgumentException("Lab ID should be provided and positive");
        }
        if (pageNum < 0) {
            throw new IllegalArgumentException("Page number cannot be negative");
        }
        if (pageSize <= 0) {
            throw new IllegalArgumentException("Page size must be positive");
        }

        log.debug("Getting submissions for lab ID: {} (page: {}, size: {})",
                labId, pageNum, pageSize);

        SubmissionProto.SubmissionList submissionList =
                submissionServiceClient.getSubmissions(labId, pageNum, pageSize);

        List<SubmissionResponse> submissions = submissionList.getSubmissionsList().stream()
                .map(submission -> {
                    UserResponse owner = userService.getUserByIdSafe(submission.getOwnerId());
                    SubmissionProto.AssetList assets = submissionServiceClient.listAssets(submission.getSubmissionId());
                    List<SubmissionAssetResponse> assetResponses = assets.getAssetsList().stream()
                            .map(this::convertAssetToResponse)
                            .toList();
                    return convertSubmissionToResponse(submission, owner, assetResponses);
                })
                .toList();

        return SubmissionListResponse.builder()
                .submissions(submissions)
                .totalCount(submissionList.getTotalCount())
                .build();
    }

    public SubmissionListResponse getSubmissionsByUserId(Long userId, Integer pageNum, Integer pageSize) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("User ID should be provided and positive");
        }
        if (pageNum < 1) {
            throw new IllegalArgumentException("Page number should be natural");
        }
        if (pageSize <= 0) {
            throw new IllegalArgumentException("Page size must be positive");
        }

        log.debug("Getting submissions for user ID: {} (page: {}, size: {})",
                userId, pageNum, pageSize);

        SubmissionProto.SubmissionList submissionList =
                submissionServiceClient.getSubmissionsByUser(userId, pageNum, pageSize);

        List<SubmissionResponse> submissions = submissionList.getSubmissionsList().stream()
                .map(submission -> {
                    SubmissionProto.AssetList assets = submissionServiceClient.listAssets(submission.getSubmissionId());
                    List<SubmissionAssetResponse> assetResponses = assets.getAssetsList().stream()
                            .map(this::convertAssetToResponse)
                            .toList();
                    return convertSubmissionToResponse(submission, null, assetResponses);
                })
                .toList();

        return SubmissionListResponse.builder()
                .submissions(submissions)
                .totalCount(submissionList.getTotalCount())
                .build();
    }

    public DeleteSubmissionResponse deleteSubmission(Long submissionId, Long userId) {
        log.debug("Deleting submission with ID: {} by user: {}", submissionId, userId);

        SubmissionProto.Submission submission = submissionServiceClient.getSubmission(submissionId);
        // Check user ownership
        if (submission.getOwnerId() != userId) {
            log.warn("User {} attempted to delete submission {} owned by {}",
                    userId, submissionId, submission.getOwnerId());
            throw new ForbiddenAccessException("You don't have permission to delete this submission");
        }

        // Delete submission
        boolean deleted = submissionServiceClient.deleteSubmission(submissionId);
        if (!deleted) {
            throw new RuntimeException("Failed to delete submission");
        }

        log.info("Successfully deleted submission {} by user {}", submissionId, userId);
        return DeleteSubmissionResponse.builder()
                .success(true)
                .message("Submission deleted successfully")
                .build();
    }

    protected void setSubmissionStatus(Long submissionId, SubmissionProto.Status status) {
        log.debug("Setting submission ID: {} status to {}", submissionId, status);
        SubmissionProto.Submission submission = submissionServiceClient.getSubmission(submissionId);
        if (submission == null) {
            throw new IllegalArgumentException("Submission not found");
        }
        if (submission.getStatus() == SubmissionProto.Status.ACCEPTED) {
            throw new SubmissionIsAlreadyGradedException("Submission is already graded");
        }
        SubmissionProto.UpdateSubmissionRequest request = SubmissionProto.UpdateSubmissionRequest.newBuilder()
                .setSubmissionId(submissionId)
                .setStatus(status)
                .build();
        submissionServiceClient.updateSubmission(request);
        log.info("Successfully set submission ID: {} status to {}", submissionId, status);
    }

    protected SubmissionProto.Status getSubmissionStatus(Long submissionId) {
        log.debug("Getting status for submission ID: {}", submissionId);
        SubmissionProto.Submission submission = submissionServiceClient.getSubmission(submissionId);
        if (submission == null) {
            throw new IllegalArgumentException("Submission not found");
        }
        log.info("Submission ID: {} has status: {}", submissionId, submission.getStatus());
        return  submission.getStatus();
    }
    protected Long getSubmissionOwnerId(Long submissionId) {
        log.debug("Getting owner ID for submission ID: {}", submissionId);
        SubmissionProto.Submission submission = submissionServiceClient.getSubmission(submissionId);
        if (submission == null) {
            throw new IllegalArgumentException("Submission not found");
        }
        log.info("Submission ID: {} is owned by user ID: {}", submissionId, submission.getOwnerId());
        return submission.getOwnerId();
    }

    // File validation methods
    private void validateSubmissionFiles(MultipartFile[] files) {
        if (files == null || files.length == 0) {
            return; // Skip validation for empty file arrays
        }

        for (MultipartFile file : files) {
            validateSubmissionFile(file);
        }
    }

    protected void validateSubmissionFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return; // Skip validation for empty files
        }

        if (file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()) {
            throw new IllegalArgumentException("File name cannot be empty");
        }

        if (file.getSize() > uploadConfig.getMaxFileSize()) {
            throw new IllegalArgumentException(String.format("File size exceeds maximum limit of %d bytes",
                    uploadConfig.getMaxFileSize()));
        }

        log.debug("File validation passed for: {}", file.getOriginalFilename());
    }

    private SubmissionResponse convertSubmissionToResponse(
            SubmissionProto.Submission submission,
            UserResponse owner,
            List<SubmissionAssetResponse> assets) {
        return SubmissionResponse.builder()
                .submissionId(submission.getSubmissionId())
                .labId(submission.getLabId())
                .owner(owner)
                .text(submission.getText())
                .createdAt(submission.getCreatedAt().toString())
                .updatedAt(submission.getUpdatedAt().toString())
                .status(submission.getStatus().name())
                .assets(assets)
                .build();
    }

    private SubmissionAssetResponse convertAssetToResponse(SubmissionProto.Asset asset) {
        return SubmissionAssetResponse.builder()
                .assetId(asset.getAssetId())
                .submissionId(asset.getSubmissionId())
                .filename(asset.getFilename())
                .totalSize(asset.getFilesize())
                .uploadDate(asset.getUploadDate().toString())
                .build();
    }
}

