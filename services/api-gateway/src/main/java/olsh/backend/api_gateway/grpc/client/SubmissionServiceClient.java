package olsh.backend.api_gateway.grpc.client;

import com.google.protobuf.ByteString;
import io.grpc.Channel;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import olsh.backend.api_gateway.config.UploadFileConfiguration;
import olsh.backend.api_gateway.exception.AssetUploadException;
import olsh.backend.api_gateway.exception.LabNotFoundException;
import olsh.backend.api_gateway.exception.SubmissionNotFoundException;
import olsh.backend.api_gateway.grpc.proto.SubmissionProto.*;
import olsh.backend.api_gateway.grpc.proto.SubmissionServiceGrpc;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
public class SubmissionServiceClient {

    private final SubmissionServiceGrpc.SubmissionServiceStub asyncStub;
    private final SubmissionServiceGrpc.SubmissionServiceBlockingStub blockingStub;
    private final UploadFileConfiguration uploadConfig;

    public SubmissionServiceClient(GrpcChannelFactory channelFactory, UploadFileConfiguration uploadConfig) {
        Channel channel = channelFactory.createChannel("lab-service");
        this.asyncStub = SubmissionServiceGrpc.newStub(channel);
        this.blockingStub = SubmissionServiceGrpc.newBlockingStub(channel);
        this.uploadConfig = uploadConfig;
    }

    // ========== Submission Management ==========

    public Submission createSubmission(CreateSubmissionRequest request) {
        log.debug("Calling submission-service gRPC CreateSubmission for lab ID: {}, owner: {}", request.getLabId(),
                request.getOwnerId());
        try {
            Submission response = blockingStub.createSubmission(request);
            log.debug("Successfully created submission via gRPC with ID: {}", response.getSubmissionId());
            return response;
        } catch (Exception e) {
            log.error("Error calling CreateSubmission gRPC: {}", e.getMessage(), e);
            if (e.getMessage().contains("NOT_FOUND")) {
                throw new LabNotFoundException(String.format(
                        "Cannot create submission for non existing lab with labId: ", request.getLabId()));
            }
            throw new RuntimeException("Failed to create submission via gRPC", e);
        }
    }

    public Submission getSubmission(Long submissionId) {
        log.debug("Calling gRPC GetSubmission for submission ID: {}", submissionId);
        try {
            GetSubmissionRequest request = GetSubmissionRequest.newBuilder().setSubmissionId(submissionId).build();
            Submission response = blockingStub.getSubmission(request);
            log.debug("Successfully retrieved submission via gRPC with ID: {}", response.getSubmissionId());
            return response;
        } catch (Exception e) {
            log.error("Error calling GetSubmission gRPC for ID {}: {}", submissionId, e.getMessage(), e);
            if (e.getMessage().contains("NOT_FOUND")) {
                throw new SubmissionNotFoundException(String.format("Submission with id=%d not found", submissionId));
            }
            throw new RuntimeException("Failed to get submission via gRPC", e);
        }
    }

    public SubmissionList getSubmissions(Long labId, Integer page, Integer limit) {
        log.debug("Calling gRPC GetSubmissions for lab ID: {}, page: {}, limit: {}", labId, page, limit);
        try {
            GetSubmissionsRequest request =
                    GetSubmissionsRequest.newBuilder().setLabId(labId).setPageNumber(page).setPageSize(limit).build();
            SubmissionList response = blockingStub.getSubmissions(request);
            log.debug("Successfully retrieved {} submissions via gRPC (total: {})", response.getSubmissionsCount(),
                    response.getTotalCount());
            return response;
        } catch (Exception e) {
            if (e.getMessage().contains("NOT_FOUND")) {
                log.warn("No submissions found for lab ID: {}", labId);
                return null; // No attachments found for this feedback
            }
            log.error("Error calling GetSubmissions gRPC for lab ID {}: {}", labId, e.getMessage(), e);
            throw new RuntimeException("Failed to get submissions via gRPC", e);
        }
    }

    public SubmissionList getSubmissionsByUser(Long userId, Integer page, Integer limit) {
        log.debug("Calling gRPC GetSubmissionsByUser for user ID: {}, page: {}, limit: {}", userId, page, limit);
        try {
            GetUsersSubmissionsRequest request =
                    GetUsersSubmissionsRequest.newBuilder().setUserId(userId).setPageNumber(page).setPageSize(limit).build();
            SubmissionList response = blockingStub.getUsersSubmissions(request);
            log.debug("Successfully retrieved {} submissions for user ID {} via gRPC (total: {})",
                    response.getSubmissionsCount(), userId, response.getTotalCount());
            return response;
        } catch (Exception e) {
            log.error("Error calling GetSubmissionsByUser gRPC for user ID {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to get submissions by user via gRPC", e);
        }
    }

    public Submission updateSubmission(UpdateSubmissionRequest request) {
        log.debug("Calling gRPC UpdateSubmission for submission ID: {}", request.getSubmissionId());
        try {
            Submission response = blockingStub.updateSubmission(request);
            log.debug("Successfully updated submission via gRPC with ID: {}", response.getSubmissionId());
            return response;
        } catch (Exception e) {
            log.error("Error calling UpdateSubmission gRPC for ID {}: {}", request.getSubmissionId(), e.getMessage(),
                    e);
            if (e.getMessage().contains("NOT_FOUND")) {
                throw new SubmissionNotFoundException(String.format("Submission with id=%d not found",
                        request.getSubmissionId()));
            }
            throw new RuntimeException("Failed to update submission via gRPC", e);
        }
    }

    public boolean deleteSubmission(Long submissionId) {
        log.debug("Calling gRPC DeleteSubmission for submission ID: {}", submissionId);
        try {
            DeleteSubmissionRequest request =
                    DeleteSubmissionRequest.newBuilder().setSubmissionId(submissionId).build();
            DeleteSubmissionResponse response = blockingStub.deleteSubmission(request);
            boolean success = response.getSuccess();
            log.debug("DeleteSubmission gRPC call completed with success: {}", success);
            return success;
        } catch (Exception e) {
            if (e.getMessage().contains("NOT_FOUND")) {
                throw new SubmissionNotFoundException(String.format("Submission with id=%d not found", submissionId));
            }
            log.error("Error calling DeleteSubmission gRPC for ID {}: {}", submissionId, e.getMessage(), e);
            throw new RuntimeException("Failed to delete submission via gRPC", e);
        }
    }

    // ========== Asset Management ==========

    public Asset uploadAsset(Long submissionId, MultipartFile file) {
        log.debug("Starting asset upload for submission ID: {}, filename: {}, size: {} bytes", submissionId,
                file.getOriginalFilename(), file.getSize());
        try {
            CompletableFuture<Asset> future = new CompletableFuture<>();
            StreamObserver<UploadAssetRequest> requestObserver = createUploadStream(future);

            sendAssetMetadata(requestObserver, submissionId, file);
            long totalSent = streamFileContent(requestObserver, file);
            requestObserver.onCompleted();

            Asset result = future.get(uploadConfig.getTimeoutSeconds(), TimeUnit.SECONDS);
            log.info("Successfully uploaded asset: ID={}, filename={}, size={} bytes", result.getAssetId(),
                    file.getOriginalFilename(), totalSent);
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssetUploadException("Upload interrupted: " + e.getMessage());
        } catch (ExecutionException e) {
            throw new AssetUploadException("Upload execution failed: " + e.getMessage());
        } catch (TimeoutException e) {
            throw new AssetUploadException("Upload timed out after " + uploadConfig.getTimeoutSeconds() + " seconds");
        } catch (IOException e) {
            throw new AssetUploadException("Failed to read file content: " + e.getMessage());
        } catch (Exception e) {
            throw new AssetUploadException("Unexpected error during asset upload: " + e.getMessage());
        }
    }

    public AssetList listAssets(Long submissionId) {
        log.debug("Listing assets for submission ID: {}", submissionId);
        try {
            ListAssetsRequest request = ListAssetsRequest.newBuilder().setSubmissionId(submissionId).build();
            AssetList response = blockingStub.listAssets(request);
            log.debug("Successfully listed {} assets for submission ID: {}", response.getTotalCount(), submissionId);
            return response;
        } catch (Exception e) {
            if (e.getMessage().contains("NOT_FOUND")) {
                log.warn("No assets found for submission ID: {}", submissionId);
                return AssetList.newBuilder().setTotalCount(0).build(); // No assets found
            }
            log.error("Failed to list assets for submission ID: {}", submissionId, e);
            throw new RuntimeException("Failed to list assets via gRPC", e);
        }
    }

    public byte[] downloadAsset(Long assetId) {
        log.debug("Downloading asset with ID: {}", assetId);
        try {
            DownloadAssetRequest request = DownloadAssetRequest.newBuilder().setAssetId(assetId).build();

            java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
            java.util.Iterator<DownloadAssetResponse> responseIterator = blockingStub.downloadAsset(request);

            // First response should contain asset metadata
            if (responseIterator.hasNext()) {
                DownloadAssetResponse first = responseIterator.next();
                if (first.hasAsset()) {
                    log.debug("Asset metadata received: filename={}, size={}", first.getAsset().getFilename(),
                            first.getAsset().getFilesize());
                }
            }

            // Subsequent responses contain file chunks
            while (responseIterator.hasNext()) {
                DownloadAssetResponse response = responseIterator.next();
                if (response.hasChunk()) {
                    outputStream.write(response.getChunk().toByteArray());
                }
            }

            byte[] result = outputStream.toByteArray();
            log.debug("Successfully downloaded asset ID: {}, size: {} bytes", assetId, result.length);
            return result;
        } catch (Exception e) {
            log.error("Failed to download asset ID: {}", assetId, e);
            throw new RuntimeException("Failed to download asset via gRPC", e);
        }
    }

    public boolean deleteAsset(Long assetId) {
        log.debug("Calling gRPC DeleteAsset for asset ID: {}", assetId);
        try {
            DeleteAssetRequest request = DeleteAssetRequest.newBuilder().setAssetId(assetId).build();
            DeleteAssetResponse response = blockingStub.deleteAsset(request);
            boolean success = response.getSuccess();
            log.debug("DeleteAsset gRPC call completed with success: {}", success);
            return success;
        } catch (Exception e) {
            log.error("Error calling DeleteAsset gRPC for ID {}: {}", assetId, e.getMessage(), e);
            throw new RuntimeException("Failed to delete asset via gRPC", e);
        }
    }

    // ========== Private Helper Methods ==========

    private StreamObserver<UploadAssetRequest> createUploadStream(CompletableFuture<Asset> future) {
        return asyncStub.uploadAsset(new StreamObserver<Asset>() {
            @Override
            public void onNext(Asset asset) {
                log.debug("Received asset response with ID: {}", asset.getAssetId());
                future.complete(asset);
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

    private void sendAssetMetadata(StreamObserver<UploadAssetRequest> requestObserver, Long submissionId,
                                   MultipartFile file) {
        UploadAssetMetadata metadata =
                UploadAssetMetadata.newBuilder().setSubmissionId(submissionId).setFilename(file.getOriginalFilename()).setFilesize(file.getSize()).build();

        UploadAssetRequest metadataRequest = UploadAssetRequest.newBuilder().setMetadata(metadata).build();

        requestObserver.onNext(metadataRequest);
        log.debug("Sent metadata: filename={}, size={} bytes", file.getOriginalFilename(), file.getSize());
    }

    private long streamFileContent(StreamObserver<UploadAssetRequest> requestObserver, MultipartFile file) throws IOException {
        byte[] buffer = new byte[uploadConfig.getChunkSize()];
        long totalSent = 0;

        try (InputStream inputStream = file.getInputStream()) {
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                UploadAssetRequest chunkRequest = UploadAssetRequest.newBuilder().setChunk(ByteString.copyFrom(buffer
                        , 0, bytesRead)).build();
                requestObserver.onNext(chunkRequest);
                totalSent += bytesRead;
                log.trace("Sent chunk of {} bytes", bytesRead);
            }
        }

        log.debug("Finished streaming file content: {} bytes total", totalSent);
        return totalSent;
    }
}
