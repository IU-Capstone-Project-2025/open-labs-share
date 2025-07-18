package olsh.backend.api_gateway.grpc.client;

import com.google.protobuf.ByteString;
import io.grpc.Channel;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import olsh.backend.api_gateway.config.UploadFileConfiguration;
import olsh.backend.api_gateway.exception.ArticleNotFoundException;
import olsh.backend.api_gateway.exception.AssetUploadException;
import olsh.backend.api_gateway.exception.GrpcError;
import olsh.backend.api_gateway.grpc.proto.ArticleProto.*;
import olsh.backend.api_gateway.grpc.proto.ArticleServiceGrpc;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.http.HttpStatus;
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

    /**
     * Creates a new article via gRPC.
     *
     * @param request The request containing article details
     * @return The created Article object
     */
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

    /**
     * Uploads an asset (file) for a specific article.
     *
     * @param articleId The ID of the article to associate the asset with
     * @param file      The file to upload
     * @return The uploaded Asset object containing metadata
     */
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

    /**
     * Creates a stream observer for uploading assets.
     *
     * @param future The CompletableFuture to complete with the Asset response
     * @return The StreamObserver for handling upload requests
     */
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

    /**
     * Sends metadata about the asset being uploaded.
     *
     * @param requestObserver The StreamObserver to send requests
     * @param articleId       The ID of the article associated with the asset
     * @param file            The file being uploaded
     */
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

    /**
     * Streams the file content in chunks to the gRPC server.
     *
     * @param requestObserver The StreamObserver to send requests
     * @param file            The file to upload
     * @return The total number of bytes sent
     * @throws IOException If an error occurs while reading the file
     */
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

    /**
     * Retrieves an article by its ID via gRPC.
     *
     * @param articleId The ID of the article to retrieve
     * @return The Article object containing details
     * @throws ArticleNotFoundException if the article does not exist
     */
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

    /**
     * Retrieves a paginated list of articles via gRPC.
     *
     * @param request The request containing pagination details
     * @return A list of articles with pagination metadata
     */
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

    /**
     * Retrieves a list of articles by user ID via gRPC.
     *
     * @param request The request containing the user ID
     * @return A list of articles authored by the specified user
     */
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

    /**
     * Deletes an article by its ID via gRPC.
     *
     * @param articleId The ID of the article to delete
     * @return true if deletion was successful, false otherwise
     * @throws ArticleNotFoundException if the article does not exist
     */
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

    /**
     * Retrieves the asset associated with an article by its ID via gRPC.
     *
     * @param articleId The ID of the article to retrieve the asset for
     * @return The Asset object containing metadata
     * @throws RuntimeException if the asset cannot be retrieved
     */
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

    /**
     * Retrieves the number of articles via gRPC.
     *
     * @return The number of articles
     * @throws GrpcError if the gRPC call fails
     */
    public Integer articlesCount() {
        log.debug("Calling gRPC GetArticlesCount to retrieve total article count");
        try {
            GetArticlesCountResponse response =
                    blockingStub.getArticlesCount(GetArticlesCountRequest.newBuilder().build());
            log.debug("Successfully retrieved article count: {}", response.getTotalCount());
            return response.getTotalCount();
        } catch (StatusRuntimeException e) {
            log.error("Error calling GetArticlesCount gRPC: {}", e.getMessage(), e);
            throw new GrpcError(HttpStatus.INTERNAL_SERVER_ERROR, e.getStatus().getCode().name(), e.getMessage());
        }
    }

}
