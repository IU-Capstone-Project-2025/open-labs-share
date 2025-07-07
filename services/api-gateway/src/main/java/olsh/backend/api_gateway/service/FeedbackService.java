package olsh.backend.api_gateway.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import olsh.backend.api_gateway.config.UploadFileConfiguration;
import olsh.backend.api_gateway.dto.request.CreateFeedbackRequest;
import olsh.backend.api_gateway.dto.response.*;
import olsh.backend.api_gateway.exception.AssetUploadException;
import olsh.backend.api_gateway.exception.FeedbackNotFoundException;
import olsh.backend.api_gateway.exception.ForbiddenAccessException;
import olsh.backend.api_gateway.grpc.client.FeedbackServiceClient;
import olsh.backend.api_gateway.grpc.proto.FeedbackProto;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackServiceClient feedbackClient;
    private final UserService userService;
    private final UploadFileConfiguration uploadConfig;

    /**
     * Creates a new feedback with optional file attachments.
     */
    public FeedbackResponse createFeedback(CreateFeedbackRequest request, Long reviewerId) {
        log.info("Creating feedback for submission {} by reviewer {}", request.getSubmissionId(), reviewerId);
        List<MultipartFile> files = Arrays.asList(request.getFiles());
        if (request.getFiles() == null) {
            files = null;
        } else if (files.size() == 1 && files.getFirst().isEmpty()) {
            log.warn("Received empty file (because of presence of 'files' field), skipping validation");
            files = null;
        } else {
            log.debug("Received {} files for feedback", request.getFiles().length);
            files = Arrays.asList(request.getFiles());
            validateFiles(files);
        }
        String feedbackId = registerFeedback(request, reviewerId).getId();
        uploadAssetsForFeedback(reviewerId, feedbackId, files);
        userService.incrementLabsReviewed(reviewerId);
        // TODO: Implement change of submission status either by calling the submission service or other way
        FeedbackProto.Feedback feedback = getFeedbackById(feedbackId);
        FeedbackResponse response = mapToFeedbackResponse(feedback);
        log.info("Successfully created feedback {} for submission {}", response.getId(), response.getSubmissionId());
        return response;
    }

    /**
     * Registers feedback in the system (without attachments).
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
     * Uploads attachments for a feedback if present.
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
        List<FeedbackProto.AttachmentInfo> attachments;
        if (attachmentsResponse == null || attachmentsResponse.getAttachmentsCount() == 0) {
            attachments = null; // No attachments found
        } else {
            attachments = attachmentsResponse.getAttachmentsList();
        }
        FeedbackResponse response = mapToFeedbackResponse(feedback, attachments);
        log.debug("Successfully retrieved feedback with id {}", feedbackId);
        return response;
    }

    /**
     * Gets feedback for a specific student's submission. Use if you do not know the feedback ID.
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
        FeedbackResponse response = mapToFeedbackResponse(feedback);
        log.debug("Retrieved feedback {} created by reviewer {}", response.getId(), response.getReviewer().getId());
        return response;
    }

    /**
     * Lists all feedbacks for a student with pagination.
     */
    public FeedbackListResponse listStudentFeedbacks(Long studentId, Long submissionId, Integer page, Integer limit) {
        log.info("Listing feedbacks for student {} (page: {}, limit: {})", studentId, page, limit);
        if (submissionId != null) {
            log.debug("Filtering by submission ID: {}", submissionId);
        }
        FeedbackProto.ListStudentFeedbacksRequest grpcRequest = FeedbackProto.ListStudentFeedbacksRequest.newBuilder()
                .setStudentId(studentId)
                .setSubmissionId(submissionId != null ? submissionId : 0L) // Use 0 for no filter
                .setPage(page)
                .setLimit(limit)
                .build();
        FeedbackProto.ListStudentFeedbacksResponse response = feedbackClient.listStudentFeedbacks(grpcRequest);
        List<FeedbackResponse> feedbacks = mapToFeedbackResponses(response.getFeedbacksList());
        log.debug("Retrieved {} feedbacks (total count: {})", feedbacks.size(), response.getTotalCount());
        return FeedbackListResponse.builder()
                .feedbacks(feedbacks)
                .totalCount(response.getTotalCount())
                .build();
    }

    /**
     * Lists all feedbacks created by a reviewer with pagination.
     */
    public FeedbackListResponse listReviewerFeedbacks(Long reviewerId, Long submissionId, Integer page, Integer limit) {
        log.info("Listing feedbacks by reviewer {} (page: {}, limit: {})", reviewerId, page, limit);
        if (submissionId != null) {
            log.debug("Filtering by submission ID: {}", submissionId);
        }
        FeedbackProto.ListReviewerFeedbacksRequest grpcRequest = FeedbackProto.ListReviewerFeedbacksRequest.newBuilder()
                .setReviewerId(reviewerId)
                .setSubmissionId(submissionId != null ? submissionId : 0L) // Use 0 for no filter
                .setPage(page)
                .setLimit(limit)
                .build();
        FeedbackProto.ListReviewerFeedbacksResponse response = feedbackClient.listReviewerFeedbacks(grpcRequest);
        List<FeedbackResponse> feedbacks = mapToFeedbackResponses(response.getFeedbacksList());
        log.debug("Retrieved {} feedbacks (total count: {})", feedbacks.size(), response.getTotalCount());
        return FeedbackListResponse.builder()
                .feedbacks(feedbacks)
                .totalCount(response.getTotalCount())
                .build();
    }

    /**
     * Deletes a feedback and its attachments.
     * Only the reviewer who created the feedback can delete it.
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

    private void validateFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return;
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
    }

    private List<FeedbackResponse> mapToFeedbackResponses(List<FeedbackProto.Feedback> feedbackList) {
        Map<Long, UserResponse> userCache = new HashMap<>();
        List<FeedbackResponse> responses = new ArrayList<>();
        for (FeedbackProto.Feedback feedback : feedbackList) {
            UserResponse student = userCache.computeIfAbsent(feedback.getStudentId(), userService::getUserByIdSafe);
            UserResponse reviewer = userCache.computeIfAbsent(feedback.getReviewerId(), userService::getUserByIdSafe);
            FeedbackResponse response = FeedbackResponse.builder()
                    .id(feedback.getId())
                    .submissionId(feedback.getSubmissionId())
                    .content(feedback.getContent())
                    .student(student)
                    .reviewer(reviewer)
                    .createdAt(TimestampConverter.convertTimestampToIso(feedback.getCreatedAt()))
                    .updatedAt(TimestampConverter.convertTimestampToIso(feedback.getUpdatedAt()))
                    .build();
            responses.add(response);
        }
        return responses;
    }

    private FeedbackResponse mapToFeedbackResponse(FeedbackProto.Feedback feedback) {
        return mapToFeedbackResponse(feedback, null);
    }

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
                .attachments(buildAssetResponse(attachments, feedback.getId()))
                .createdAt(TimestampConverter.convertTimestampToIso(feedback.getCreatedAt()))
                .updatedAt(TimestampConverter.convertTimestampToIso(feedback.getUpdatedAt()))
                .build();
    }

    private List<FeedbackAssetResponse> buildAssetResponse(List<FeedbackProto.AttachmentInfo> attachments, String feedbackId) {
        if (attachments == null || attachments.isEmpty()) {
            return Collections.emptyList();
        }
        return attachments.stream()
                .map(info -> buildAssetResponse(info, feedbackId))
                .collect(Collectors.toList());
    }

    private FeedbackAssetResponse buildAssetResponse(FeedbackProto.AttachmentInfo info, String feedbackId) {
        return FeedbackAssetResponse.builder()
                .feedbackId(feedbackId)
                .filename(info.getFilename())
                .contentType(info.getContentType())
                .totalSize(info.getSize())
                .build();
    }

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
