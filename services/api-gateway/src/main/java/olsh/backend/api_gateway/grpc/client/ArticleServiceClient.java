package olsh.backend.api_gateway.grpc.client;

import com.google.protobuf.ByteString;
import io.grpc.Channel;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import olsh.backend.api_gateway.config.UploadFileConfiguration;
import olsh.backend.api_gateway.exception.ArticleNotFoundException;
import olsh.backend.api_gateway.exception.AssetUploadException;
import olsh.backend.api_gateway.grpc.proto.ArticleProto.*;
import olsh.backend.api_gateway.grpc.proto.ArticleServiceGrpc;
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
public class ArticleServiceClient {

    private final ArticleServiceGrpc.ArticleServiceStub asyncStub; // async
    private final ArticleServiceGrpc.ArticleServiceBlockingStub blockingStub; // sync
    private final UploadFileConfiguration uploadConfig;

    public ArticleServiceClient(GrpcChannelFactory channelFactory, UploadFileConfiguration uploadConfig) {
        Channel channel = channelFactory.createChannel("article-service");
        this.asyncStub = ArticleServiceGrpc.newStub(channel);
        this.blockingStub = ArticleServiceGrpc.newBlockingStub(channel);
        this.uploadConfig = uploadConfig;
    }

    public Article createArticle(CreateArticleRequest request) {
        log.debug("Calling article-service gRPC CreateArticle for title: {}", request.getTitle());

        try {
            Article response = blockingStub.createArticle(request);
            log.debug("Successfully created article via gRPC with ID: {}", response.getArticleId());
            return response;
        } catch (Exception e) {
            log.error("Error calling CreateArticle gRPC: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create article via gRPC", e);
        }
    }

    public Asset uploadAsset(Long articleId, MultipartFile file) {
        log.debug("Starting asset upload for article ID: {}, filename: {}, size: {} bytes",
                articleId, file.getOriginalFilename(), file.getSize());

        try {
            CompletableFuture<Asset> future = new CompletableFuture<>();
            StreamObserver<UploadAssetRequest> requestObserver = createUploadStream(future);

            sendMetadata(requestObserver, articleId, file);
            long totalSent = streamFileContent(requestObserver, file);
            requestObserver.onCompleted();

            Asset result = future.get(uploadConfig.getTimeoutSeconds(), TimeUnit.SECONDS);

            log.info("Successfully uploaded asset: ID={}, filename={}, size={} bytes",
                    result.getAssetId(), file.getOriginalFilename(), totalSent);
            return result;
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

    private void sendMetadata(StreamObserver<UploadAssetRequest> requestObserver, Long articleId, MultipartFile file) {
        UploadAssetMetadata metadata = UploadAssetMetadata.newBuilder()
                .setArticleId(articleId)
                .setFilename(file.getOriginalFilename())
                .setFilesize(file.getSize())
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
        long lastLoggedMB = 0;

        try (InputStream inputStream = file.getInputStream()) {
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                ByteString chunk = ByteString.copyFrom(buffer, 0, bytesRead);

                UploadAssetRequest chunkRequest = UploadAssetRequest.newBuilder()
                        .setChunk(chunk)
                        .build();

                requestObserver.onNext(chunkRequest);
                totalSent += bytesRead;

                // Log progress every MB
                long currentMB = totalSent / (1024 * 1024);
                if (currentMB > lastLoggedMB) {
                    log.debug("Upload progress: {} MB / {} MB", currentMB, file.getSize() / (1024 * 1024));
                    lastLoggedMB = currentMB;
                }
            }
        }

        log.debug("Finished streaming file content. Total sent: {} bytes", totalSent);
        return totalSent;
    }

    public Article getArticle(Long articleId) {
        log.debug("Calling gRPC GetArticle for article ID: {}", articleId);

        try {
            GetArticleRequest request = GetArticleRequest.newBuilder()
                    .setArticleId(articleId)
                    .build();

            Article response = blockingStub.getArticle(request);

            log.debug("Successfully retrieved article via gRPC with ID: {}", response.getArticleId());
            return response;

        } catch (Exception e) {
            log.error("Error calling GetArticle gRPC for ID {}: {}", articleId, e.getMessage(), e);
            if (e.getMessage().contains("NOT_FOUND")) {
                throw new ArticleNotFoundException(String.format("Article with id=%d not found", articleId));
            }
            throw new RuntimeException("Failed to get article via gRPC", e);
        }
    }

    public ArticleList getArticles(GetArticlesRequest request) {
        log.debug("Calling gRPC GetArticles for page: {}, limit: {}", request.getPageNumber(), request.getPageSize());
        try {
            ArticleList response = blockingStub.getArticles(request);
            log.debug("Successfully retrieved {} articles via gRPC (total: {})",
                    response.getArticlesCount(), response.getTotalCount());
            return response;

        } catch (Exception e) {
            log.error("Error calling GetArticles gRPC: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get articles via gRPC", e);
        }
    }

    public ArticleList getUserArticles(GetArticlesByUserIdRequest request) {
        log.debug("Calling gRPC GetUserArticles for user ID: {}", request.getUserId());
        try {
            ArticleList response = blockingStub.getArticlesByUserId(request);
            log.debug("Successfully retrieved {} user articles via gRPC (total: {})",
                    response.getArticlesCount(), response.getTotalCount());
            return response;
        } catch (Exception e) {
            log.error("Error calling GetUserArticles gRPC: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get user articles via gRPC", e);
        }
    }

    public boolean deleteArticle(Long articleId) {
        log.debug("Calling gRPC DeleteArticle for article ID: {}", articleId);

        try {
            DeleteArticleRequest request = DeleteArticleRequest.newBuilder()
                    .setArticleId(articleId)
                    .build();

            DeleteArticleResponse response = blockingStub.deleteArticle(request);

            boolean success = response.getSuccess();
            log.debug("DeleteArticle gRPC call completed with success: {}", success);
            return success;

        } catch (Exception e) {
            if (e.getMessage().contains("NOT_FOUND")) {
                throw new ArticleNotFoundException(String.format("Article with id=%d not found", articleId));
            }
            log.error("Error calling DeleteArticle gRPC for ID {}: {}", articleId, e.getMessage(), e);
            throw new RuntimeException("Failed to delete article via gRPC", e);
        }
    }

    public Asset getAssetByArticleId(Long articleId) {
        log.debug("Calling gRPC GetAssetByArticleId for article ID: {}", articleId);
        try {
            ListAssetsRequest request = ListAssetsRequest.newBuilder()
                    .setArticleId(articleId)
                    .build();
            AssetList response = blockingStub.listAssets(request);
            Asset asset = response.getAssetsList().getFirst();
            log.debug("Successfully retrieved asset for article ID: {}", articleId);
            return asset;
        } catch (Exception e) {
            log.error("Error calling GetAssetByArticleId gRPC for article ID {}: {}", articleId, e.getMessage(), e);
            throw new RuntimeException("Failed to get asset by article ID via gRPC", e);
        }
    }

}
