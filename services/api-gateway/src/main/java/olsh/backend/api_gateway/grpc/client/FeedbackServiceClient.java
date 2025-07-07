package olsh.backend.api_gateway.grpc.client;

import com.google.protobuf.ByteString;
import io.grpc.Channel;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import olsh.backend.api_gateway.config.UploadFileConfiguration;
import olsh.backend.api_gateway.exception.AssetUploadException;
import olsh.backend.api_gateway.exception.FeedbackNotFoundException;
import olsh.backend.api_gateway.grpc.proto.FeedbackProto.*;
import olsh.backend.api_gateway.grpc.proto.FeedbackServiceGrpc;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Client for interacting with the Feedback gRPC service.
 * Handles all feedback-related operations including feedback management and file attachments.
 */
@Slf4j
@Service
public class FeedbackServiceClient {

    private final FeedbackServiceGrpc.FeedbackServiceStub asyncStub;
    private final FeedbackServiceGrpc.FeedbackServiceBlockingStub blockingStub;
    private final UploadFileConfiguration uploadConfig;

    public FeedbackServiceClient(GrpcChannelFactory channelFactory, UploadFileConfiguration uploadConfig) {
        Channel channel = channelFactory.createChannel("feedback-service");
        this.asyncStub = FeedbackServiceGrpc.newStub(channel);
        this.blockingStub = FeedbackServiceGrpc.newBlockingStub(channel);
        this.uploadConfig = uploadConfig;
    }

    // ========== Feedback Management ==========

    /**
     * Creates a new feedback for a submission.
     *
     * @param request Contains reviewer ID, student ID, submission ID, title, and content
     * @return The created feedback with all details
     * @throws RuntimeException if the gRPC call fails
     */
    public Feedback createFeedback(CreateFeedbackRequest request) {
        log.debug("Calling feedback-service gRPC CreateFeedback for submission ID: {}, reviewer: {}",
                request.getSubmissionId(), request.getReviewerId());
        try {
            Feedback response = blockingStub.createFeedback(request);
            log.debug("Successfully created feedback via gRPC with ID: {}", response.getId());
            return response;
        } catch (Exception e) {
            log.error("Error calling CreateFeedback gRPC: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create feedback via gRPC", e);
        }
    }

    /**
     * Updates an existing feedback's title and/or content.
     * Only the reviewer who created the feedback can update it.
     *
     * @param request Contains reviewer ID, feedback ID, and optional title and content updates
     * @return The updated feedback with all details
     * @throws FeedbackNotFoundException if the feedback doesn't exist
     * @throws RuntimeException          if the gRPC call fails
     */
    public Feedback updateFeedback(UpdateFeedbackRequest request) {
        log.debug("Calling gRPC UpdateFeedback for feedback ID: {}, reviewer: {}", request.getId(),
                request.getReviewerId());
        try {
            Feedback response = blockingStub.updateFeedback(request);
            log.debug("Successfully updated feedback via gRPC with ID: {}", response.getId());
            return response;
        } catch (Exception e) {
            log.error("Error calling UpdateFeedback gRPC: {}", e.getMessage(), e);
            if (e.getMessage().contains("NOT_FOUND")) {
                throw new FeedbackNotFoundException(String.format("Feedback with id=%s not found", request.getId()));
            }
            throw new RuntimeException("Failed to update feedback via gRPC", e);
        }
    }

/**
 * Retrieves feedback for a specific student's submission.
 *
 * @param request gRPC request containing studentId and submissionId
 * @return The feedback for the specified submission
 * @throws FeedbackNotFoundException if no feedback exists for the submission
 * @throws RuntimeException if the gRPC call fails
 */
public Feedback getStudentFeedback(GetStudentFeedbackRequest request) {
    log.debug("Calling gRPC GetStudentFeedback for student ID: {}, submission ID: {}", request.getStudentId(), request.getSubmissionId());
    try {
        Feedback response = blockingStub.getStudentFeedback(request);
        log.debug("Successfully retrieved feedback via gRPC with ID: {}", response.getId());
        return response;
    } catch (Exception e) {
        log.error("Error calling GetStudentFeedback gRPC: {}", e.getMessage(), e);
        if (e.getMessage().contains("NOT_FOUND")) {
            throw new FeedbackNotFoundException("Feedback not found for the specified submission");
        }
        throw new RuntimeException("Failed to get student feedback via gRPC", e);
    }
}

    /**
     * Lists feedbacks for a student with pagination.
     * Optionally filters by submission ID.
     *
     * @param request gRPC request containing student ID, optional submission ID, page number, and limit
     * @return Paginated list of feedbacks with total count
     * @throws RuntimeException if the gRPC call fails
     */
    public ListStudentFeedbacksResponse listStudentFeedbacks(ListStudentFeedbacksRequest request) {
        log.debug("Calling gRPC ListStudentFeedbacks for student ID: {}, submission ID: {}, page: {}, limit: {}",
                request.getStudentId(), request.getSubmissionId(), request.getPage(), request.getLimit());
        try {
            ListStudentFeedbacksResponse response = blockingStub.listStudentFeedbacks(request);
            log.debug("Successfully retrieved {} feedbacks for student via gRPC (total: {})",
                    response.getFeedbacksCount(), response.getTotalCount());
            return response;
        } catch (Exception e) {
            log.error("Error calling ListStudentFeedbacks gRPC for student ID {}: {}", request.getStudentId(),
                    e.getMessage(), e);
            throw new RuntimeException("Failed to list student feedbacks via gRPC", e);
        }
    }
    /**
     * Lists feedbacks created by a reviewer with pagination.
     * Optionally filters by submission ID.
     *
     * @param request   gRPC request containing reviewer ID, optional submission ID, page number, and limit
     * @return Paginated list of feedbacks with total count
     * @throws RuntimeException if the gRPC call fails
     */
   public ListReviewerFeedbacksResponse listReviewerFeedbacks(ListReviewerFeedbacksRequest request) {
       log.debug("Calling gRPC ListReviewerFeedbacks for reviewer ID: {}, submission ID: {}, page: {}, limit: {}",
               request.getReviewerId(), request.getSubmissionId(), request.getPage(), request.getLimit());
       try {
           ListReviewerFeedbacksResponse response = blockingStub.listReviewerFeedbacks(request);
           log.debug("Successfully retrieved {} feedbacks for reviewer via gRPC (total: {})",
                   response.getFeedbacksCount(), response.getTotalCount());
           return response;
       } catch (Exception e) {
           log.error("Error calling ListReviewerFeedbacks gRPC for reviewer ID {}: {}", request.getReviewerId(), e.getMessage(), e);
           throw new RuntimeException("Failed to list reviewer feedbacks via gRPC", e);
       }
   }

    /**
     * Deletes a feedback. Only the reviewer who created the feedback can delete it.
     *
     * @param request gRPC request containing feedback ID and reviewer ID
     * @return true if deletion was successful, false otherwise
     * @throws FeedbackNotFoundException if the feedback doesn't exist
     * @throws RuntimeException          if the gRPC call fails
     */
    public boolean deleteFeedback(DeleteFeedbackRequest request) {
        log.debug("Calling gRPC DeleteFeedback for feedback ID: {}, reviewer ID: {}", request.getId(), request.getReviewerId());
        try {
            DeleteFeedbackResponse response = blockingStub.deleteFeedback(request);
            log.debug("DeleteFeedback gRPC call completed with success: {}", response.getSuccess());
            return response.getSuccess();
        } catch (Exception e) {
            if (e.getMessage().contains("NOT_FOUND")) {
                throw new FeedbackNotFoundException(String.format("Feedback with id=%s not found", request.getId()));
            }
            log.error("Error calling DeleteFeedback gRPC for ID {}: {}", request.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to delete feedback via gRPC", e);
        }
    }

    /**
     * Retrieves a specific feedback by its ID.
     *
     * @param feedbackId feedback ID
     * @return The feedback if found
     * @throws FeedbackNotFoundException if the feedback does not exist
     * @throws RuntimeException if the gRPC call fails
     */
    public Feedback getFeedbackById(String feedbackId) {
        log.debug("Calling gRPC GetFeedbackById for feedback ID: {}", feedbackId);
        try {
            GetFeedbackByIdRequest request = GetFeedbackByIdRequest.newBuilder()
                    .setId(feedbackId)
                    .build();
            Feedback response = blockingStub.getFeedbackById(request);
            log.debug("Successfully retrieved feedback via gRPC with ID: {}", response.getId());
            return response;
        } catch (Exception e) {
            log.error("Error calling GetFeedbackById gRPC: {}", e.getMessage(), e);
            if (e.getMessage().contains("NOT_FOUND")) {
                throw new FeedbackNotFoundException(String.format("Feedback with id=%s not found", feedbackId));
            }
            throw new RuntimeException("Failed to get feedback via gRPC", e);
        }
    }

    // ========== Attachment Management ==========

    /**
     * Uploads a file attachment for a feedback.
     * Streams the file in chunks to handle large files efficiently.
     *
     * @param reviewerId ID of the reviewer uploading the attachment
     * @param feedbackId ID of the feedback to attach the file to
     * @param file       The file to upload
     * @return Upload response containing filename and success status
     * @throws AssetUploadException if the upload fails
     */
    public UploadAttachmentResponse uploadAttachment(Long reviewerId, String feedbackId, MultipartFile file) {
        log.debug("Starting attachment upload for feedback ID: {}, filename: {}, size: {} bytes",
                feedbackId, file.getOriginalFilename(), file.getSize());
        try {
            CompletableFuture<UploadAttachmentResponse> future = new CompletableFuture<>();
            StreamObserver<UploadAttachmentRequest> requestObserver = createUploadStream(future);

            // Send metadata first
            sendAttachmentMetadata(requestObserver, reviewerId, feedbackId, file);
            
            // Stream file content
            streamFileContent(requestObserver, file);
            
            // Mark the stream as complete
            requestObserver.onCompleted();
            
            // Wait for the response with timeout
            UploadAttachmentResponse result = future.get(uploadConfig.getTimeoutSeconds(), TimeUnit.SECONDS);
            log.info("Successfully uploaded attachment: filename={}, size={} bytes",
                    result.getFilename(), result.getSize());
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssetUploadException("Upload interrupted: " + e.getMessage());
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            log.error("Upload execution failed", cause);
            throw new AssetUploadException("Upload failed: " + cause.getMessage());
        } catch (TimeoutException e) {
            throw new AssetUploadException("Upload timed out after " + uploadConfig.getTimeoutSeconds() + " seconds");
        } catch (IOException e) {
            throw new AssetUploadException("Failed to read file content: " + e.getMessage());
        } catch (Exception e) {
            throw new AssetUploadException("Unexpected error during attachment upload: " + e.getMessage());
        }
    }

    /**
     * Lists all attachments for a specific feedback.
     *
     * @param feedbackId ID of the feedback
     * @return List of attachment information including filenames, sizes, and content types
     * @throws RuntimeException if the gRPC call fails
     */
    public ListAttachmentsResponse listAttachments(String feedbackId) {
        log.debug("Listing attachments for feedback ID: {}", feedbackId);
        try {
            ListAttachmentsRequest request = ListAttachmentsRequest.newBuilder()
                    .setFeedbackId(feedbackId)
                    .build();
            ListAttachmentsResponse response = blockingStub.listAttachments(request);
            log.debug("Successfully listed {} attachments for feedback ID: {}",
                    response.getAttachmentsCount(), feedbackId);
            return response;
        } catch (Exception e) {
            log.error("Failed to list attachments for feedback ID: {}", feedbackId, e);
            if (e.getMessage().contains("NOT_FOUND")) {
                return null; // No attachments found for this feedback
            }
            throw new RuntimeException("Failed to list attachments via gRPC", e);
        }
    }

    /**
     * Deletes an attachment from a feedback.
     * Only the reviewer who created the feedback can delete its attachments.
     */
    public boolean deleteAttachment(DeleteAttachmentRequest request) {
        log.debug("Calling gRPC DeleteAttachment for feedback ID: {}, filename: {}", request.getFeedbackId(),
                request.getFilename());
        try {
            DeleteAttachmentResponse response = blockingStub.deleteAttachment(request);
            log.debug("DeleteAttachment gRPC call completed with success: {}", response.getSuccess());
            return response.getSuccess();
        } catch (Exception e) {
            log.error("Error calling DeleteAttachment gRPC for feedback ID {} and filename {}: {}",
                    request.getFeedbackId(), request.getFilename(), e.getMessage(), e);
            throw new RuntimeException("Failed to delete attachment via gRPC", e);
        }
    }

    /**
     * Downloads an attachment from a feedback.
     * Streams the file in chunks to handle large files efficiently.
     *
     * @param feedbackId ID of the feedback
     * @param filename   Name of the file to download
     * @return The file content as a byte array
     * @throws RuntimeException if the gRPC call fails
     */
    public byte[] downloadAttachment(String feedbackId, String filename) {
        log.debug("Downloading attachment for feedback ID: {}, filename: {}", feedbackId, filename);
        try {
            DownloadAttachmentRequest request = DownloadAttachmentRequest.newBuilder()
                    .setFeedbackId(feedbackId)
                    .setFilename(filename)
                    .build();

            java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
            java.util.Iterator<DownloadAttachmentResponse> responseIterator = blockingStub.downloadAttachment(request);

            // First response should contain attachment info
            if (responseIterator.hasNext()) {
                DownloadAttachmentResponse first = responseIterator.next();
                if (first.hasInfo()) {
                    log.debug("Attachment info received: filename={}, size={}",
                            first.getInfo().getFilename(), first.getInfo().getSize());
                }
            }

            // Subsequent responses contain file chunks
            while (responseIterator.hasNext()) {
                DownloadAttachmentResponse response = responseIterator.next();
                if (response.hasChunk()) {
                    outputStream.write(response.getChunk().toByteArray());
                }
            }

            byte[] result = outputStream.toByteArray();
            log.debug("Successfully downloaded attachment: filename={}, size={} bytes", filename, result.length);
            return result;
        } catch (Exception e) {
            log.error("Failed to download attachment for feedback ID: {}, filename: {}", feedbackId, filename, e);
            throw new RuntimeException("Failed to download attachment via gRPC", e);
        }
    }

    /**
     * Gets MinIO location information for feedback attachments.
     * Can request location for a specific file or all files in the feedback.
     *
     * @param feedbackId ID of the feedback
     * @param filename   Optional specific filename to get location for
     * @return Location information including MinIO bucket, object path, and endpoint
     * @throws RuntimeException if the gRPC call fails
     */
    public GetAttachmentLocationResponse getAttachmentLocation(String feedbackId, String filename) {
        log.debug("Getting attachment location for feedback ID: {}, filename: {}", feedbackId, filename);
        try {
            GetAttachmentLocationRequest.Builder requestBuilder = GetAttachmentLocationRequest.newBuilder()
                    .setFeedbackId(feedbackId);

            if (filename != null) {
                requestBuilder.setFilename(filename);
            }

            GetAttachmentLocationResponse response = blockingStub.getAttachmentLocation(requestBuilder.build());
            log.debug("Successfully retrieved location info for {} attachments", response.getAttachmentsCount());
            return response;
        } catch (Exception e) {
            log.error("Failed to get attachment location for feedback ID: {}, filename: {}", feedbackId, filename, e);
            throw new RuntimeException("Failed to get attachment location via gRPC", e);
        }
    }

    // ========== Private Helper Methods ==========

    /**
     * Creates a stream observer for handling file upload responses.
     */
    private StreamObserver<UploadAttachmentRequest> createUploadStream(CompletableFuture<UploadAttachmentResponse> future) {
        return asyncStub.uploadAttachment(new StreamObserver<UploadAttachmentResponse>() {
            @Override
            public void onNext(UploadAttachmentResponse response) {
                log.debug("Received upload response for file: {}", response.getFilename());
                future.complete(response);
            }

            @Override
            public void onError(Throwable t) {
                log.error("gRPC upload stream error: {}", t.getMessage(), t);
                future.completeExceptionally(t);
            }

            @Override
            public void onCompleted() {
                log.debug("Upload stream completed successfully");
            }
        });
    }

    /**
     * Sends metadata about a file being uploaded.
     */
    private void sendAttachmentMetadata(StreamObserver<UploadAttachmentRequest> requestObserver,
                                        Long reviewerId, String feedbackId, MultipartFile file) {
        AttachmentMetadata metadata = AttachmentMetadata.newBuilder()
                .setReviewerId(reviewerId)
                .setFeedbackId(feedbackId)
                .setFilename(file.getOriginalFilename())
                .setContentType(file.getContentType())
                .setTotalSize(file.getSize())
                .build();

        UploadAttachmentRequest metadataRequest = UploadAttachmentRequest.newBuilder()
                .setMetadata(metadata)
                .build();

        requestObserver.onNext(metadataRequest);
        log.debug("Sent metadata: filename={}, size={} bytes, content-type={}",
                file.getOriginalFilename(), file.getSize(), file.getContentType());
    }

    /**
     * Streams file content in chunks.
     *
     * @return Total number of bytes sent
     */
    private long streamFileContent(StreamObserver<UploadAttachmentRequest> requestObserver, MultipartFile file)
            throws IOException {
        byte[] buffer = new byte[uploadConfig.getChunkSize()];
        long totalSent = 0;

        try (InputStream inputStream = file.getInputStream()) {
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                UploadAttachmentRequest chunkRequest = UploadAttachmentRequest.newBuilder()
                        .setChunk(ByteString.copyFrom(buffer, 0, bytesRead))
                        .build();
                requestObserver.onNext(chunkRequest);
                totalSent += bytesRead;
                log.trace("Sent chunk of {} bytes", bytesRead);
            }
        }

        log.debug("Finished streaming file content: {} bytes total", totalSent);
        return totalSent;
    }
} 