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

    /**
     * Creates a new submission for a lab.
     *
     * @param request  the request containing submission details
     * @param ownerId  the ID of the user creating the submission
     * @return response containing submission metadata and status
     */
    public CreateSubmissionResponse createSubmission(CreateSubmissionRequest request, Long ownerId) {
        log.debug("Creating submission for lab ID: {} by owner: {}", request.getLabId(), ownerId);
        validateSubmissionFiles(request.getFiles());
        labService.validateLabExists(request.getLabId());
        if (labService.validateLabAuthorId(request.getLabId(), ownerId)){
            throw new ForbiddenAccessException("You cannot submit to your own lab");
        }
        SubmissionResponse submission = register(request, ownerId);
        List<SubmissionAssetResponse> assets = uploadAssets(
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

    /**
     * Registers a new submission in the system.
     *
     * @param request  the request containing submission details
     * @param ownerId  the ID of the user creating the submission
     * @return response containing submission metadata
     */
    private SubmissionResponse register(CreateSubmissionRequest request, Long ownerId) {
        SubmissionProto.CreateSubmissionRequest protoRequest = SubmissionProto.CreateSubmissionRequest.newBuilder()
                .setLabId(request.getLabId())
                .setOwnerId(ownerId)
                .setText(request.getTextComment() != null ? request.getTextComment() : "")
                .build();
        log.debug("Registering submission with lab ID: {} and owner ID: {}", request.getLabId(), ownerId);
        SubmissionProto.Submission submission = submissionServiceClient.createSubmission(protoRequest);
        log.debug("Successfully registered submission with ID: {}", submission.getSubmissionId());
        UserResponse owner = userService.getUserByIdSafe(ownerId);
        return mapSubmissionToResponse(submission, owner, new ArrayList<>());
    }

    /**
     * Uploads assets for a submission.
     *
     * @param submissionId the ID of the submission
     * @param files        the files to upload
     * @return list of uploaded asset responses
     */
    private List<SubmissionAssetResponse> uploadAssets(Long submissionId, MultipartFile[] files) {
        List<SubmissionAssetResponse> assetResponses = new ArrayList<>();
        log.debug("Uploading assets for submission ID: {}", submissionId);
        if (files != null) {
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    SubmissionProto.Asset asset = submissionServiceClient.uploadAsset(submissionId, file);
                    assetResponses.add(mapAssetToResponse(asset));
                }
            }
        }
        log.debug("Successfully uploaded {} assets for submission ID: {}", assetResponses.size(), submissionId);
        return assetResponses;
    }

    /**
     * Retrieves a submission by its ID.
     *
     * @param submissionId the ID of the submission
     * @return response containing submission metadata and assets
     */
    public SubmissionResponse getById(Long submissionId) {
        if (submissionId == null || submissionId <= 0) {
            throw new IllegalArgumentException("Submission ID should be provided and positive");
        }

        log.debug("Getting submission with ID: {}", submissionId);

        SubmissionProto.Submission submission = submissionServiceClient.getSubmission(submissionId);
        UserResponse owner = userService.getUserByIdSafe(submission.getOwnerId());

        SubmissionProto.AssetList assetList = submissionServiceClient.listAssets(submissionId);
        List<SubmissionAssetResponse> assets = assetList.getAssetsList().stream()
                .map(this::mapAssetToResponse)
                .toList();

        return mapSubmissionToResponse(submission, owner, assets);
    }

    /**
     * Retrieves a list of submissions for a specific lab.
     *
     * @param labId    the ID of the lab
     * @param pageNum  the page number for pagination
     * @param pageSize the number of submissions per page
     * @return response containing a list of submissions and total count
     */
    public SubmissionListResponse getByLabId(Long labId, Integer pageNum, Integer pageSize) {
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
        SubmissionListResponse response = buildSubmissionListResponse(submissionList);
        log.info("Retrieved {} submissions for lab ID: {}", submissionList.getSubmissionsCount(), labId);
        return response;
    }

    /**
     * Retrieves a list of submissions by a specific user.
     *
     * @param userId   the ID of the user
     * @param pageNum  the page number for pagination
     * @param pageSize the number of submissions per page
     * @return response containing a list of submissions and total count
     */
    public SubmissionListResponse getByUserId(Long userId, Integer pageNum, Integer pageSize) {
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
        SubmissionListResponse response = buildSubmissionListResponse(submissionList);
        log.info("Retrieved {} submissions for user ID: {}", submissionList.getSubmissionsCount(), userId);
        return response;
    }

    /**
     * Retrieves a list of submissions for review by a specific user.
     *
     * @param userId   the ID of the user
     * @param pageNum  the page number for pagination
     * @param pageSize the number of submissions per page
     * @return response containing a list of submissions and total count
     */
    public SubmissionListResponse getForReview(Long userId, Integer pageNum, Integer pageSize) {
        if (pageNum < 1) {
            throw new IllegalArgumentException("Page number should be natural");
        }
        if (pageSize <= 0) {
            throw new IllegalArgumentException("Page size must be positive");
        }
        log.debug("Getting submissions for review by user ID: {} (page: {}, size: {})",
                userId, pageNum, pageSize);
        SubmissionProto.SubmissionList submissionList =
                submissionServiceClient.getForReview(userId, pageNum, pageSize);
        SubmissionListResponse response = buildSubmissionListResponse(submissionList);
        log.info("Retrieved {} submissions for review by user ID: {}",
                submissionList.getSubmissionsCount(), userId);
        return response;
    }

    /**
     * Deletes a submission by its ID.
     *
     * @param submissionId the ID of the submission to delete
     * @param userId       the ID of the user requesting deletion
     * @return response indicating success or failure
     */
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

    /**
     * Sets the status of a submission.
     *
     * @param submissionId the ID of the submission
     * @param status       the new status to set
     */
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

    /**
     * Gets the status of a submission.
     *
     * @param submissionId the ID of the submission
     * @return the status of the submission
     */
    protected SubmissionProto.Status getSubmissionStatus(Long submissionId) {
        log.debug("Getting status for submission ID: {}", submissionId);
        SubmissionProto.Submission submission = submissionServiceClient.getSubmission(submissionId);
        if (submission == null) {
            throw new IllegalArgumentException("Submission not found");
        }
        log.info("Submission ID: {} has status: {}", submissionId, submission.getStatus());
        return  submission.getStatus();
    }

    /**
     * Gets the owner ID of a submission.
     *
     * @param submissionId the ID of the submission
     * @return the ID of the user who owns the submission
     */
    protected Long getSubmissionOwnerId(Long submissionId) {
        log.debug("Getting owner ID for submission ID: {}", submissionId);
        SubmissionProto.Submission submission = submissionServiceClient.getSubmission(submissionId);
        if (submission == null) {
            throw new IllegalArgumentException("Submission not found");
        }
        log.info("Submission ID: {} is owned by user ID: {}", submissionId, submission.getOwnerId());
        return submission.getOwnerId();
    }

    /**
     * Validates the files in a submission request.
     *
     * @param files the files to validate
     */
    private void validateSubmissionFiles(MultipartFile[] files) {
        if (files == null || files.length == 0) {
            return; // Skip validation for empty file arrays
        }

        for (MultipartFile file : files) {
            validateSubmissionFile(file);
        }
    }

    /**
     * Validates a single submission file.
     *
     * @param file the file to validate
     * @throws IllegalArgumentException if the file is invalid
     */
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

    /**
     * Maps a SubmissionProto.Submission to a SubmissionResponse.
     *
     * @param submission the submission proto object
     * @param owner      the owner of the submission
     * @param assets     the list of assets associated with the submission
     * @return a SubmissionResponse object
     */
    private SubmissionResponse mapSubmissionToResponse(
            SubmissionProto.Submission submission,
            UserResponse owner,
            List<SubmissionAssetResponse> assets) {
        return SubmissionResponse.builder()
                .submissionId(submission.getSubmissionId())
                .labId(submission.getLabId())
                .owner(owner)
                .text(submission.getText())
                .createdAt(TimestampConverter.convertTimestampToIso(submission.getCreatedAt()))
                .updatedAt(TimestampConverter.convertTimestampToIso(submission.getUpdatedAt()))
                .status(submission.getStatus().name())
                .assets(assets)
                .build();
    }

    /**
     * Maps a SubmissionProto.Asset to a SubmissionAssetResponse.
     *
     * @param asset the asset proto object
     * @return a SubmissionAssetResponse object
     */
    private SubmissionAssetResponse mapAssetToResponse(SubmissionProto.Asset asset) {
        return SubmissionAssetResponse.builder()
                .assetId(asset.getAssetId())
                .submissionId(asset.getSubmissionId())
                .filename(asset.getFilename())
                .totalSize(asset.getFilesize())
                .uploadDate(TimestampConverter.convertTimestampToIso(asset.getUploadDate()))
                .build();
    }

    /**
     * Builds a SubmissionListResponse from a SubmissionProto.SubmissionList.
     *
     * @param submissionList the submission list proto object
     * @return a SubmissionListResponse object
     */
    private SubmissionListResponse buildSubmissionListResponse(
            SubmissionProto.SubmissionList submissionList) {
        List<SubmissionResponse> submissions = submissionList.getSubmissionsList().stream()
                .map(submission -> {
                    UserResponse owner = userService.getUserByIdSafe(submission.getOwnerId());
                    SubmissionProto.AssetList assets = submissionServiceClient.listAssets(submission.getSubmissionId());
                    List<SubmissionAssetResponse> assetResponses = assets.getAssetsList().stream()
                            .map(this::mapAssetToResponse)
                            .toList();
                    return mapSubmissionToResponse(submission, owner, assetResponses);
                })
                .toList();
        return SubmissionListResponse.builder()
                .submissions(submissions)
                .totalCount(submissionList.getTotalCount())
                .build();
    }
}

