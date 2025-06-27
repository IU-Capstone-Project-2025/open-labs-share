package olsh.backend.api_gateway.grpc.client;

import com.google.protobuf.ByteString;
import io.grpc.Channel;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import olsh.backend.api_gateway.config.UploadFileConfiguration;
import olsh.backend.api_gateway.exception.AssetUploadException;
import olsh.backend.api_gateway.exception.LabNotFoundException;
import olsh.backend.api_gateway.grpc.proto.LabProto.*;
import olsh.backend.api_gateway.grpc.proto.LabServiceGrpc;
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
public class LabServiceClient {

    private final LabServiceGrpc.LabServiceStub asyncStub;
    private final LabServiceGrpc.LabServiceBlockingStub blockingStub;
    private final UploadFileConfiguration uploadConfig;

    public LabServiceClient(GrpcChannelFactory channelFactory, UploadFileConfiguration uploadConfig) {
        Channel channel = channelFactory.createChannel("lab-service");
        this.asyncStub = LabServiceGrpc.newStub(channel);
        this.blockingStub = LabServiceGrpc.newBlockingStub(channel);
        this.uploadConfig = uploadConfig;
    }

    public Lab createLab(CreateLabRequest request) {
        log.debug("Calling lab-service gRPC CreateLab for title: {}", request.getTitle());
        try {
            Lab response = blockingStub.createLab(request);
            log.debug("Successfully created lab via gRPC with ID: {}", response.getLabId());
            return response;
        } catch (Exception e) {
            log.error("Error calling CreateLab gRPC: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create lab via gRPC", e);
        }
    }

    public Lab getLab(Long labId) {
        log.debug("Calling gRPC GetLab for lab ID: {}", labId);
        try {
            GetLabRequest request = GetLabRequest.newBuilder()
                    .setLabId(labId)
                    .build();

            Lab response = blockingStub.getLab(request);
            log.debug("Successfully retrieved lab via gRPC with ID: {}", response.getLabId());
            return response;
        } catch (Exception e) {
            log.error("Error calling GetLab gRPC for ID {}: {}", labId, e.getMessage(), e);
            if (e.getMessage().contains("NOT_FOUND")) {
                throw new LabNotFoundException(String.format("Lab with id=%d not found", labId));
            }
            throw new RuntimeException("Failed to get lab via gRPC", e);
        }
    }

    public LabList getLabs(Integer page, Integer limit) {
        log.debug("Calling gRPC GetLabs for page: {}, limit: {}", page, limit);
        try {
            GetLabsRequest request = GetLabsRequest.newBuilder()
                    .setPageNumber(page)
                    .setPageSize(limit)
                    .build();

            LabList response = blockingStub.getLabs(request);
            log.debug("Successfully retrieved {} labs via gRPC (total: {})",
                    response.getLabsCount(), response.getTotalCount());
            return response;
        } catch (Exception e) {
            log.error("Error calling GetLabs gRPC: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get labs via gRPC", e);
        }
    }

    public boolean deleteLab(Long labId) {
        log.debug("Calling gRPC DeleteLab for lab ID: {}", labId);
        try {
            DeleteLabRequest request = DeleteLabRequest.newBuilder()
                    .setLabId(labId)
                    .build();

            DeleteLabResponse response = blockingStub.deleteLab(request);
            boolean success = response.getSuccess();
            log.debug("DeleteLab gRPC call completed with success: {}", success);
            return success;
        } catch (Exception e) {
            if (e.getMessage().contains("NOT_FOUND")) {
                throw new LabNotFoundException(String.format("Lab with id=%d not found", labId));
            }
            log.error("Error calling DeleteLab gRPC for ID {}: {}", labId, e.getMessage(), e);
            throw new RuntimeException("Failed to delete lab via gRPC", e);
        }
    }

    public void uploadAsset(Long labId, MultipartFile file) {
        log.debug("Starting asset upload for lab ID: {}, filename: {}, size: {} bytes",
                labId, file.getOriginalFilename(), file.getSize());

        try {
            CompletableFuture<Asset> future = new CompletableFuture<>();
            StreamObserver<UploadAssetRequest> requestObserver = createUploadStream(future);

            sendMetadata(requestObserver, labId, file);
            long totalSent = streamFileContent(requestObserver, file);
            requestObserver.onCompleted();

            Asset result = future.get(uploadConfig.getTimeoutSeconds(), TimeUnit.SECONDS);
            log.info("Successfully uploaded asset: ID={}, filename={}, size={} bytes",
                    result.getAssetId(), file.getOriginalFilename(), totalSent);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssetUploadException(e.getMessage());
        } catch (ExecutionException e) {
            throw new AssetUploadException(e.getMessage());
        } catch (TimeoutException e) {
            throw new AssetUploadException("Upload timed out after " + uploadConfig.getTimeoutSeconds() + " seconds");
        } catch (IOException e) {
            throw new AssetUploadException("Failed to read file content");
        } catch (Exception e) {
            throw e;
        }
    }

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

    private void sendMetadata(StreamObserver<UploadAssetRequest> requestObserver, Long labId, MultipartFile file) {
        UploadAssetMetadata metadata = UploadAssetMetadata.newBuilder()
                .setLabId(labId)
                .setFilename(file.getOriginalFilename())
                .setTotalSize(file.getSize())
                .build();

        UploadAssetRequest metadataRequest = UploadAssetRequest.newBuilder()
                .setMetadata(metadata)
                .build();

        requestObserver.onNext(metadataRequest);
        log.debug("Sent metadata: filename={}, size={} bytes", file.getOriginalFilename(), file.getSize());
    }

    private long streamFileContent(StreamObserver<UploadAssetRequest> requestObserver, MultipartFile file) throws IOException {
        byte[] buffer = new byte[uploadConfig.getChunkSize()];
        long totalSent = 0;

        try (InputStream inputStream = file.getInputStream()) {
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                UploadAssetRequest chunkRequest = UploadAssetRequest.newBuilder()
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

    // New methods for asset management
    public AssetList listAssets(Long labId) {
        log.debug("Listing assets for lab ID: {}", labId);
        
        ListAssetsRequest request = ListAssetsRequest.newBuilder()
                .setLabId(labId)
                .build();
        
        try {
            AssetList response = blockingStub.listAssets(request);
            log.debug("Successfully listed {} assets for lab ID: {}", response.getTotalCount(), labId);
            return response;
        } catch (Exception e) {
            log.error("Failed to list assets for lab ID: {}", labId, e);
            throw e;
        }
    }

    public byte[] downloadAsset(Long assetId) {
        log.debug("Downloading asset with ID: {}", assetId);
        
        DownloadAssetRequest request = DownloadAssetRequest.newBuilder()
                .setAssetId(assetId)
                .build();
        
        try {
            java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
            
            java.util.Iterator<DownloadAssetResponse> responseIterator = blockingStub.downloadAsset(request);
            
            // First response should contain asset metadata
            if (responseIterator.hasNext()) {
                DownloadAssetResponse first = responseIterator.next();
                if (first.hasAsset()) {
                    log.debug("Asset metadata received: filename={}, size={}", 
                        first.getAsset().getFilename(), first.getAsset().getTotalSize());
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
            throw new RuntimeException("Failed to download asset", e);
        }
    }
}

