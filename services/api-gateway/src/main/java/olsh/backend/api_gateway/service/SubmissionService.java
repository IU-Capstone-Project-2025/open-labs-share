package olsh.backend.api_gateway.service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import olsh.backend.api_gateway.config.UploadFileConfiguration;
import olsh.backend.api_gateway.dto.request.CreateSubmissionRequest;
import olsh.backend.api_gateway.dto.response.*;
import olsh.backend.api_gateway.exception.ForbiddenAccessException;
import olsh.backend.api_gateway.grpc.client.SubmissionServiceClient;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final SubmissionServiceClient submissionServiceClient;
    private final UploadFileConfiguration uploadConfig;
    private final UserService userService;

    public CreateSubmissionResponse createSubmission(CreateSubmissionRequest request, Long ownerId) {
        log.debug("Creating submission for lab ID: {} by owner: {}", request.getLabId(), ownerId);

        if (request.getFiles() != null) {
            for (MultipartFile file : request.getFiles()) {
                validateSubmissionFile(file);
            }
        }

        // Placeholder implementation
        log.debug("TODO: Call gRPC CreateSubmission method");


    }

    public SubmissionResponse getSubmissionById(Long submissionId) {
        if (submissionId == null || submissionId <= 0) {
            throw new IllegalArgumentException("Submission ID should be provided and positive");
        }

        log.debug("Getting submission with ID: {}", submissionId);

        // TODO: Get submission from gRPC service
        log.debug("TODO: Call gRPC GetSubmission method for ID: {}", submissionId);

        // TODO: Get submission assets
        log.debug("TODO: Call gRPC ListAssets method for submission ID: {}", submissionId);

        // TODO: Get owner information
        log.debug("TODO: Get user information for submission owner");

        // Placeholder response
        return SubmissionResponse.builder()
                .submissionId(submissionId)
                .labId(1L)
                .ownerId(1L)
                .ownerName("John")
                .ownerSurname("Doe")
                .text("Sample submission text")
                .status("NOT_GRADED")
                .createdAt("2024-03-15T14:30:00Z")
                .updatedAt("2024-03-15T14:30:00Z")
                .assets(new ArrayList<>())
                .build();
    }

    public SubmissionListResponse getSubmissionsByLabId(Long labId) {
        if (labId == null || labId <= 0) {
            throw new IllegalArgumentException("Lab ID should be provided and positive");
        }

        log.debug("Getting submissions for lab ID: {}", labId);

        // TODO: Get submissions from gRPC service
        log.debug("TODO: Call gRPC GetSubmissions method for lab ID: {}", labId);

        // TODO: For each submission, get its assets
        log.debug("TODO: Get assets for each submission");

        // TODO: Get owner information for each submission (with caching)
        log.debug("TODO: Get user information for submission owners");

        // Placeholder response
        return SubmissionListResponse.builder()
                .submissions(new ArrayList<>())
                .totalCount(0L)
                .build();
    }

    public DeleteSubmissionResponse deleteSubmission(Long submissionId, Long userId) {
        log.debug("Deleting submission with ID: {} by user: {}", submissionId, userId);

        // TODO: Get submission to check ownership
        log.debug("TODO: Get submission to verify ownership");

        // TODO: Check if user owns the submission
        log.debug("TODO: Verify user {} owns submission {}", userId, submissionId);

        // TODO: Delete submission via gRPC
        log.debug("TODO: Call gRPC DeleteSubmission method");

        // Placeholder response
        return DeleteSubmissionResponse.builder()
                .success(true)
                .message("Submission deleted successfully!")
                .build();
    }

    // File validation methods
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

    // TODO: Add utility methods for converting proto objects to DTOs
    // private SubmissionResponse convertSubmissionToResponse(SubmissionProto.Submission submission, UserResponse owner, List<AssetResponse> assets)
    // private AssetResponse convertAssetToResponse(SubmissionProto.Asset asset)
}

