package olsh.backend.api_gateway.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import olsh.backend.api_gateway.config.UploadFileConfiguration;
import olsh.backend.api_gateway.dto.request.CreateFeedbackRequest;
import olsh.backend.api_gateway.dto.response.*;
import olsh.backend.api_gateway.exception.AssetUploadException;
import olsh.backend.api_gateway.exception.FeedbackNotFoundException;
import olsh.backend.api_gateway.exception.ForbiddenAccessException;
import olsh.backend.api_gateway.exception.SubmissionIsAlreadyGradedException;
import olsh.backend.api_gateway.grpc.client.FeedbackServiceClient;
import olsh.backend.api_gateway.grpc.proto.FeedbackProto;
import olsh.backend.api_gateway.grpc.proto.SubmissionProto;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackServiceClient feedbackClient;
    private final SubmissionService submissionService;
    private final UserService userService;
    private final UploadFileConfiguration uploadConfig;

    /**
     * Creates feedback for a student's submission.
     * Validates the submission status and user access before creating feedback.
     * Uploads attachments if provided.
     *
     * @param request    The request containing feedback details and attachments.
     * @param reviewerId The ID of the reviewer creating the feedback.
     * @return The created feedback response.
     */
    public FeedbackResponse createFeedback(CreateFeedbackRequest request, Long reviewerId) {
        log.info("Creating feedback for submission {} by reviewer {}", request.getSubmissionId(), reviewerId);
        List<MultipartFile> files = validateFiles(request);
        validateSubmissionStatus(request.getSubmissionId());
        validateUserAccess(reviewerId, request.getSubmissionId());
        String feedbackId = registerFeedback(request, reviewerId).getId();
        uploadAssetsForFeedback(reviewerId, feedbackId, files);
        userService.incrementLabsReviewed(reviewerId);
        log.info("Successfully created feedback {} for submission {}", feedbackId, request.getSubmissionId());
        submissionService.setSubmissionStatus(request.getSubmissionId(), SubmissionProto.Status.ACCEPTED);
        FeedbackResponse response = getFeedback(feedbackId);
        return response;
    }

    private void validateSubmissionStatus(Long submissionId) {
        log.debug("Validating lab status for submission {}", submissionId);
        if (submissionId == null || submissionId <= 0) {
            log.error("Invalid submission ID: {}", submissionId);
            throw new IllegalArgumentException("Submission ID must be a positive number");
        }
        SubmissionProto.Status status = submissionService.getSubmissionStatus(submissionId);
        if (status != SubmissionProto.Status.NOT_GRADED) {
            log.warn("Submission {} is not in NOT_GRADED status, current status: {}", submissionId,
                    status);
            throw new SubmissionIsAlreadyGradedException(
                    "Submission is already graded or in progress, cannot add feedback");
        }
        log.debug("Submission {} is in NOT_GRADED status, proceeding with feedback creation", submissionId);
    }

    private void validateUserAccess(Long reviewerId, Long submissionId) {
        log.debug("Validating access for reviewer {} to submission {}", reviewerId, submissionId);
        if (Objects.equals(submissionService.getSubmissionOwnerId(submissionId), reviewerId)){
            log.warn("Reviewer {} cannot create feedback for their own submission {}", reviewerId, submissionId);
            throw new ForbiddenAccessException("You cannot create feedback for your own submission");
        }
        log.debug("Reviewer {} has access to submission {}", reviewerId, submissionId);
    }

    /**
     * Registers feedback in the system.
     * This method is responsible for creating the feedback entry in the database.
     *
     * @param request    The request containing feedback details.
     * @param reviewerId The ID of the reviewer creating the feedback.
     * @return The created feedback object.
     */
    private FeedbackProto.Feedback registerFeedback(CreateFeedbackRequest request, Long reviewerId) {
        FeedbackProto.CreateFeedbackRequest protoRequest = FeedbackProto.CreateFeedbackRequest.newBuilder()
                .setReviewerId(reviewerId)
                .setStudentId(request.getStudentId())
                .setSubmissionId(request.getSubmissionId())
                .setTitle("Empty Title") // Title is not used in the current implementation
                .setContent(request.getContent())
                .build();
        FeedbackProto.Feedback feedback = feedbackClient.createFeedback(protoRequest);
        log.debug("Feedback created with ID: {}", feedback.getId());
        return feedback;
    }

    /**
     * Uploads assets (attachments) for the feedback.
     * This method iterates through the provided files and uploads each one.
     *
     * @param reviewerId The ID of the reviewer uploading the assets.
     * @param feedbackId The ID of the feedback to which the assets are being uploaded.
     * @param files      The list of files to upload.
     */
    private void uploadAssetsForFeedback(Long reviewerId, String feedbackId, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return;
        }
        log.info("Uploading {} attachment(s) for feedback {}", files.size(), feedbackId);
        for (MultipartFile file : files) {
            if (file.getSize() == 0) {
                log.warn("Skipping empty file: {}", file.getOriginalFilename());
                continue; // Skip empty files
            }
            log.debug("Uploading file: {} (size: {} bytes)", file.getOriginalFilename(), file.getSize());
            feedbackClient.uploadAttachment(reviewerId, feedbackId, file);
            log.info("Successfully uploaded file {} for feedback {}", file.getOriginalFilename(), feedbackId);
        }
    }

    public FeedbackResponse getFeedback(String feedbackId) {
        try {
            UUID.fromString(feedbackId);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Feedback ID should be of UUID format");
        }
        log.info("Retrieving feedback with ID {}", feedbackId);
        FeedbackProto.Feedback feedback = feedbackClient.getFeedbackById(feedbackId);
        FeedbackProto.ListAttachmentsResponse attachmentsResponse = feedbackClient.listAttachments(feedbackId);
        FeedbackResponse response = mapToFeedbackResponse(feedback, attachmentsResponse.getAttachmentsList());
        log.debug("Successfully retrieved feedback with id {}", feedbackId);
        return response;
    }

    /**
     * Retrieves feedback for a specific student and submission.
     * Validates the submission ID before fetching feedback.
     *
     * @param studentId   The ID of the student.
     * @param submissionId The ID of the submission.
     * @return The feedback response for the specified student and submission.
     */
    public FeedbackResponse getStudentFeedback(Long studentId, Long submissionId) {
        log.info("Retrieving feedback for student {} and submission {}", studentId, submissionId);
        if (submissionId <= 0) {
            log.error("Submission ID must be positive");
            throw new IllegalArgumentException("Submission ID must be positive");
        }
        FeedbackProto.GetStudentFeedbackRequest grpcRequest = FeedbackProto.GetStudentFeedbackRequest.newBuilder()
                .setStudentId(studentId)
                .setSubmissionId(submissionId)
                .build();
        FeedbackProto.Feedback feedback = feedbackClient.getStudentFeedback(grpcRequest);
        FeedbackProto.ListAttachmentsResponse attachments = feedbackClient.listAttachments(feedback.getId());
        FeedbackResponse response = mapToFeedbackResponse(feedback, attachments.getAttachmentsList());
        log.debug("Retrieved feedback {} created by reviewer {}", response.getId(), response.getReviewer().getId());
        return response;
    }

    /**
     * Lists all feedbacks for a student with pagination.
     * Optionally filters by submission ID if provided.
     *
     * @param studentId   The ID of the student.
     * @param submissionId The ID of the submission (optional).
     * @param page        The page number for pagination.
     * @param limit       The number of items per page.
     * @return A response containing the list of feedbacks and total count.
     */
    public FeedbackListResponse listStudentFeedbacks(Long studentId, Long submissionId, Integer page, Integer limit) {
        log.info("Listing feedbacks for student {} (page: {}, limit: {})", studentId, page, limit);
        FeedbackProto.ListStudentFeedbacksRequest grpcRequest = FeedbackProto.ListStudentFeedbacksRequest.newBuilder()
                .setStudentId(studentId)
                .setPage(page)
                .setLimit(limit)
                .build();
        if (submissionId != null) {
            grpcRequest = grpcRequest.toBuilder()
                    .setSubmissionId(submissionId)
                    .build();
            log.debug("Filtering by submission ID: {}", submissionId);
        }
        FeedbackProto.ListStudentFeedbacksResponse response = feedbackClient.listStudentFeedbacks(grpcRequest);
        List<FeedbackResponse> feedbacks = mapToFeedbackResponses(response.getFeedbacksList());
        log.debug("Retrieved {} feedbacks (total count: {})", feedbacks.size(), response.getTotalCount());
        return FeedbackListResponse.builder()
                .feedbacks(feedbacks)
                .totalCount(response.getTotalCount())
                .build();
    }

    /**
     * Lists all feedbacks created by a specific reviewer.
     * Optionally filters by submission ID if provided.
     *
     * @param reviewerId  The ID of the reviewer.
     * @param submissionId The ID of the submission (optional).
     * @param page        The page number for pagination.
     * @param limit       The number of items per page.
     * @return A response containing the list of feedbacks and total count.
     */
    public FeedbackListResponse listReviewerFeedbacks(Long reviewerId, Long submissionId, Integer page, Integer limit) {
        log.info("Listing feedbacks by reviewer {} (page: {}, limit: {})", reviewerId, page, limit);
        FeedbackProto.ListReviewerFeedbacksRequest grpcRequest = FeedbackProto.ListReviewerFeedbacksRequest.newBuilder()
                .setReviewerId(reviewerId)
                .setPage(page)
                .setLimit(limit)
                .build();
        if (submissionId != null) {
            grpcRequest = grpcRequest.toBuilder()
                    .setSubmissionId(submissionId)
                    .build();
            log.debug("Filtering by submission ID: {}", submissionId);
        }
        FeedbackProto.ListReviewerFeedbacksResponse response = feedbackClient.listReviewerFeedbacks(grpcRequest);
        List<FeedbackResponse> feedbacks = mapToFeedbackResponses(response.getFeedbacksList());
        log.debug("Retrieved {} feedbacks (total count: {})", feedbacks.size(), response.getTotalCount());
        return FeedbackListResponse.builder()
                .feedbacks(feedbacks)
                .totalCount(response.getTotalCount())
                .build();
    }

    /**
     * Deletes feedback by its ID.
     * Only the reviewer who created the feedback can delete it.
     *
     * @param feedbackId The ID of the feedback to delete.
     * @param reviewerId The ID of the reviewer attempting to delete the feedback.
     * @return A response indicating success or failure of the deletion.
     */
    public DeleteFeedbackResponse deleteFeedback(String feedbackId, Long reviewerId) {
        log.info("Attempting to delete feedback {} by reviewer {}", feedbackId, reviewerId);
        // Verify the reviewer owns this feedback
        FeedbackProto.Feedback feedback = getFeedbackById(feedbackId);
        if (feedback.getReviewerId() != reviewerId) {
            log.warn("Unauthorized attempt to delete feedback {} by reviewer {}", feedbackId, reviewerId);
            throw new ForbiddenAccessException("Only the reviewer who created the feedback can delete it");
        }
        FeedbackProto.DeleteFeedbackRequest grpcRequest = FeedbackProto.DeleteFeedbackRequest.newBuilder()
                .setId(feedbackId)
                .setReviewerId(reviewerId)
                .build();
        boolean response = feedbackClient.deleteFeedback(grpcRequest);
        if (!response) {
            log.error("Failed to delete feedback {}", feedbackId);
            return new DeleteFeedbackResponse(false, "Failed to delete feedback");
        }
        log.info("Successfully deleted feedback {}", feedbackId);
        return new DeleteFeedbackResponse(true, "Feedback deleted successfully.");
    }

    /**
     * Downloads a feedback attachment file.
     */
    @Deprecated
    public byte[] downloadAttachment(String feedbackId, String filename) {
        log.info("Downloading attachment {} from feedback {}", filename, feedbackId);

        FeedbackProto.DownloadAttachmentRequest request = FeedbackProto.DownloadAttachmentRequest.newBuilder()
                .setFeedbackId(feedbackId)
                .setFilename(filename)
                .build();

        byte[] fileContent = feedbackClient.downloadAttachment(feedbackId, filename);
        log.debug("Successfully downloaded file {} (size: {} bytes)", filename, fileContent.length);

        return fileContent;
    }

    /**
     * Lists all attachments for a feedback.
     */
    @Deprecated
    public List<FeedbackAssetResponse> listAttachments(String feedbackId) {
        log.info("Listing attachments for feedback {}", feedbackId);

        FeedbackProto.ListAttachmentsRequest request = FeedbackProto.ListAttachmentsRequest.newBuilder()
                .setFeedbackId(feedbackId)
                .build();

        FeedbackProto.ListAttachmentsResponse response = feedbackClient.listAttachments(feedbackId);
        List<FeedbackAssetResponse> assets = response.getAttachmentsList().stream()
                .map((asset) -> buildAssetResponse(asset, feedbackId))
                .collect(Collectors.toList());

        log.debug("Found {} attachments for feedback {}", assets.size(), feedbackId);
        return assets;
    }

    /**
     * Deletes an attachment from a feedback.
     * Only the reviewer who created the feedback can delete its attachments.
     */
    @Deprecated
    public void deleteAttachment(Long reviewerId, String feedbackId, String filename) {
        log.info("Attempting to delete attachment {} from feedback {} by reviewer {}",
                filename, feedbackId, reviewerId);

        // Verify the reviewer owns this feedback
        FeedbackProto.Feedback feedback = getFeedbackById(feedbackId);
        if (feedback.getReviewerId() != reviewerId) {
            log.warn("Unauthorized attempt to delete attachment from feedback {} by reviewer {}",
                    feedbackId, reviewerId);
            throw new ForbiddenAccessException("Only the reviewer who created the feedback can delete its attachments");
        }

        FeedbackProto.DeleteAttachmentRequest request = FeedbackProto.DeleteAttachmentRequest.newBuilder()
                .setReviewerId(reviewerId)
                .setFeedbackId(feedbackId)
                .setFilename(filename)
                .build();

        boolean result = feedbackClient.deleteAttachment(request);
        if (result) {
            log.error("Failed to delete attachment {} from feedback {}", filename, feedbackId);
            throw new RuntimeException("Failed to delete attachment");
        }

        log.info("Successfully deleted attachment {} from feedback {}", filename, feedbackId);
    }

    // ========== Private Helper Methods ==========

    /**
     * Validates the files provided in the feedback request.
     * Checks for file size limits and returns a list of valid files.
     *
     * @param request The request containing files to validate.
     * @return A list of validated files.
     */
    private List<MultipartFile> validateFiles(CreateFeedbackRequest request) {
        List<MultipartFile> files;
        if (request.getFiles() == null) {
            return Collections.emptyList();
        } else {
            log.debug("Received {} files for feedback", request.getFiles().length);
            files = Arrays.asList(request.getFiles());
        }
        if (files.size() == 1 && files.getFirst().isEmpty()) {
            log.warn("Received empty file (because of presence of 'files' field), skipping validation");
            return Collections.emptyList();
        }
        log.debug("Validating {} files", files.size());
        for (MultipartFile file : files) {
            // Check file size
            if (file.getSize() > uploadConfig.getMaxFileSize()) {
                log.warn("File {} exceeds size limit of {} bytes (actual size: {})",
                        file.getOriginalFilename(), uploadConfig.getMaxFileSize(), file.getSize());
                throw new AssetUploadException(String.format(
                        "File %s exceeds maximum size limit of %d bytes",
                        file.getOriginalFilename(),
                        uploadConfig.getMaxFileSize()
                ));
            }
            log.debug("File {} ({} bytes) passed validation", file.getOriginalFilename(), file.getSize());
        }
        return files;
    }

    /**
     * Maps a list of FeedbackProto.Feedback objects to a list of FeedbackResponse objects.
     * This method handles user data retrieval and attachment mapping.
     *
     * @param feedbackList The list of FeedbackProto.Feedback objects to map.
     * @return A list of FeedbackResponse objects.
     */
    private List<FeedbackResponse> mapToFeedbackResponses(List<FeedbackProto.Feedback> feedbackList) {
        Map<Long, UserResponse> userCache = new HashMap<>();
        List<FeedbackResponse> responses = new ArrayList<>();
        for (FeedbackProto.Feedback feedback : feedbackList) {
            UserResponse student = userCache.computeIfAbsent(feedback.getStudentId(), userService::getUserByIdSafe);
            UserResponse reviewer = userCache.computeIfAbsent(feedback.getReviewerId(), userService::getUserByIdSafe);
            FeedbackProto.ListAttachmentsResponse attachmentsResponse =
                    feedbackClient.listAttachments(feedback.getId());
            List<FeedbackAssetResponse> attachments = buildAssetResponse(attachmentsResponse.getAttachmentsList(),
                    feedback.getId());
            FeedbackResponse response = FeedbackResponse.builder()
                    .id(feedback.getId())
                    .submissionId(feedback.getSubmissionId())
                    .content((
                            feedback.getContent() == null || feedback.getContent().isBlank()) ?
                            "No content!" : feedback.getContent()
                    )
                    .student(student)
                    .reviewer(reviewer)
                    .createdAt(TimestampConverter.convertTimestampToIso(feedback.getCreatedAt()))
                    .updatedAt(TimestampConverter.convertTimestampToIso(feedback.getUpdatedAt()))
                    .attachments(attachments)
                    .build();
            responses.add(response);
        }
        return responses;
    }

    /**
     * Maps a FeedbackProto.Feedback object to a FeedbackResponse object.
     * This method retrieves user data and builds the response with attachments.
     *
     * @param feedback    The FeedbackProto.Feedback object to map.
     * @param attachments The list of attachments associated with the feedback.
     * @return A FeedbackResponse object containing the mapped data.
     */
    private FeedbackResponse mapToFeedbackResponse(FeedbackProto.Feedback feedback,
                                                   List<FeedbackProto.AttachmentInfo> attachments) {
        log.debug("Building feedback response for feedback {}", feedback.getId());
        // Get user data for student and reviewer
        UserResponse student = userService.getUserByIdSafe(feedback.getStudentId());
        UserResponse reviewer = userService.getUserByIdSafe(feedback.getReviewerId());
        return FeedbackResponse.builder()
                .id(feedback.getId())
                .submissionId(feedback.getSubmissionId())
                .content(feedback.getContent())
                .student(student)
                .reviewer(reviewer)
                .attachments(
                        buildAssetResponse(attachments, feedback.getId())
                )
                .createdAt(TimestampConverter.convertTimestampToIso(feedback.getCreatedAt()))
                .updatedAt(TimestampConverter.convertTimestampToIso(feedback.getUpdatedAt()))
                .build();
    }

    /**
     * Builds a list of FeedbackAssetResponse objects from a list of AttachmentInfo objects.
     *
     * @param attachments The list of AttachmentInfo objects containing attachment details.
     * @param feedbackId  The ID of the feedback to which these attachments belong.
     * @return A list of FeedbackAssetResponse objects with attachment details.
     */
    private List<FeedbackAssetResponse> buildAssetResponse(List<FeedbackProto.AttachmentInfo> attachments,
                                                           String feedbackId) {
        if (attachments == null || attachments.isEmpty()) {
            return Collections.emptyList();
        }
        return attachments.stream()
                .map(info -> buildAssetResponse(info, feedbackId))
                .collect(Collectors.toList());
    }

    /**
     * Builds a single FeedbackAssetResponse from an AttachmentInfo object.
     *
     * @param info        The AttachmentInfo object containing attachment details.
     * @param feedbackId  The ID of the feedback to which this attachment belongs.
     * @return A FeedbackAssetResponse object with the attachment details.
     */
    private FeedbackAssetResponse buildAssetResponse(FeedbackProto.AttachmentInfo info, String feedbackId) {
        return FeedbackAssetResponse.builder()
                .feedbackId(feedbackId)
                .filename(info.getFilename())
                .contentType(info.getContentType())
                .totalSize(info.getSize())
                .build();
    }

    /**
     * Retrieves feedback by its ID.
     * Validates the UUID format and fetches the feedback from the client.
     *
     * @param feedbackId The ID of the feedback to retrieve.
     * @return The FeedbackProto.Feedback object if found.
     * @throws FeedbackNotFoundException if the feedback is not found.
     */
    private FeedbackProto.Feedback getFeedbackById(String feedbackId) {
        log.debug("Retrieving feedback by ID: {}", feedbackId);
        try {
            UUID.fromString(feedbackId); // Validate UUID format
            log.info("Fetching feedback with ID {}", feedbackId);
            return feedbackClient.getFeedbackById(feedbackId);
        } catch (Exception e) {
            log.error("Failed to retrieve feedback {}: {}", feedbackId, e.getMessage());
            throw new FeedbackNotFoundException("Feedback not found with ID: " + feedbackId);
        }
    }
}