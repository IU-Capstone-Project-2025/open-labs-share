package olsh.backend.api_gateway.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import olsh.backend.api_gateway.config.UploadFileConfiguration;
import olsh.backend.api_gateway.dto.request.CreateArticleRequest;
import olsh.backend.api_gateway.dto.request.ArticlesGetRequest;
import olsh.backend.api_gateway.dto.response.*;
import olsh.backend.api_gateway.exception.ArticleNotFoundException;
import olsh.backend.api_gateway.exception.ForbiddenAccessException;
import olsh.backend.api_gateway.grpc.client.ArticleServiceClient;
import olsh.backend.api_gateway.grpc.proto.ArticleProto;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Service for managing articles, including creation, retrieval, listing, and deletion.
 * Handles business logic and communication with the Article gRPC service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleServiceClient articleServiceClient;
    private final UploadFileConfiguration uploadConfig;
    private final UserService userService;

    /**
     * Creates a new article and uploads its PDF asset.
     * @param request Article creation request data
     * @param authorId ID of the author
     * @return Response with created article details
     */
    public CreateArticleResponse createArticle(CreateArticleRequest request, Long authorId) {
        log.debug("Creating article with title: {} for author: {}", request.getTitle(), authorId);
        validatePdfFile(request.getPdf_file());
        ArticleProto.Article article = registerArticle(request, authorId);
        if (article == null || article.getArticleId() == 0) {
            throw new RuntimeException("Failed to create article record in the database.");
        }
        ArticleProto.Asset asset = uploadAssetForArticle(article.getArticleId(), request.getPdf_file());
        ArticleResponse articleResponse = buildArticleResponse(article, userService.getUserById(authorId), asset);
        return CreateArticleResponse.builder()
                .id(article.getArticleId())
                .message("Article created successfully")
                .article(articleResponse)
                .build();
    }

    private ArticleProto.Asset uploadAssetForArticle(Long articleId, MultipartFile pdfFile) {
        try {
            ArticleProto.Asset asset = articleServiceClient.uploadAsset(articleId, pdfFile);
            log.debug("Successfully uploaded asset for article ID: {}", articleId);
            return asset;
        } catch (Exception e) {
            // If asset upload fails, we must roll back the article creation.
            log.error("Asset upload failed for article ID: {}. Attempting to roll back article creation.", articleId,
                    e);
            try {
                articleServiceClient.deleteArticle(articleId);
                log.info("Successfully rolled back (deleted) article with ID: {}", articleId);
            } catch (Exception rollbackEx) {
                // If the rollback fails, this is a critical state.
                log.error("CRITICAL: Failed to roll back article creation for article ID: {}. Orphaned article may " +
                        "exist.", articleId, rollbackEx);
                // In a real-world scenario, this should trigger a monitoring alert.
            }
            // Inform the client that the asset upload failed and the operation was rolled back.
            throw new RuntimeException("Failed to upload article asset. The article creation has been rolled back.", e);
        }
    }

    /**
     * Validates the uploaded PDF file for article creation.
     * @param file Multipart PDF file
     */
    protected void validatePdfFile(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getOriginalFilename() == null) {
            throw new IllegalArgumentException("PDF file is required");
        }

        if (!file.getOriginalFilename().toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Only PDF files are allowed");
        }

        if (file.getSize() > uploadConfig.getMaxFileSize()) {
            throw new IllegalArgumentException(String.format("File size exceeds maximum limit of %d bytes",
                    uploadConfig.getMaxFileSize()));
        }
    }

    private ArticleProto.Article registerArticle(CreateArticleRequest request, Long authorId) {
        ArticleProto.CreateArticleRequest grpcRequest = ArticleProto.CreateArticleRequest
                .newBuilder()
                .setOwnerId(authorId)
                .setTitle(request.getTitle())
                .setAbstract(request.getShort_desc())
                .build();
        ArticleProto.Article article = articleServiceClient.createArticle(grpcRequest);
        log.debug("Successfully registered article with ID: {}", article.getArticleId());
        return article;
    }

    /**
     * Retrieves an article by its ID.
     * @param articleId Article ID
     * @return Article response with details
     */
    public ArticleResponse getArticleById(Long articleId) {
        if (articleId == null || articleId == 0) {
            throw new IllegalArgumentException("ArticleId should be provided");
        }
        log.debug("Getting article with ID: {}", articleId);
        // Get article and its user from gRPC service
        ArticleProto.Article article = articleServiceClient.getArticle(articleId);
        ArticleProto.Asset asset = articleServiceClient.getAssetByArticleId(articleId);
        UserResponse author = userService.getUserById(article.getOwnerId());
        ArticleResponse response = buildArticleResponse(article, author, asset);
        log.debug("Successfully retrieved article: {}", article.getTitle());
        return response;
    }

    /**
     * Retrieves articles for a specific user (author).
     * @param id Author/user ID
     * @param page Page number
     * @param limit Page size
     * @return List response with user's articles
     */
    public ArticleListResponse getUsersArticles(long id, int page, int limit) {
        log.debug("Getting articles for author {} - page: {}, limit: {}", id, page, limit);
        ArticleProto.GetArticlesByUserIdRequest grpcRequest = ArticleProto.GetArticlesByUserIdRequest
                .newBuilder()
                .setUserId(id)
                .setPageNumber(page)
                .setPageSize(limit)
                .build();
        ArticleProto.ArticleList grpcResponse = articleServiceClient.getUserArticles(grpcRequest);
        ArticleListResponse response = buildArticleListResponse(grpcResponse.getArticlesList());
        log.debug("Successfully retrieved {} articles out of {} total",
                response.getArticles().size(), grpcResponse.getArticlesCount());
        return response;
    }

    /**
     * Retrieves a paginated list of articles, optionally filtered by tags or text.
     * @param request Articles list request with filters
     * @return List response with articles
     */
    public ArticleListResponse getArticles(ArticlesGetRequest request) {
        log.debug("Getting articles list - page: {}, limit: {}", request.getPage(), request.getLimit());
        ArticleProto.GetArticlesRequest.Builder builder = ArticleProto.GetArticlesRequest.newBuilder()
                .setPageNumber(request.getPage())
                .setPageSize(request.getLimit())
                .addAllTagsIds(request.getTagsList());
        if (!request.getText().isBlank()) {
            builder.setText(request.getText());
        }
        ArticleProto.ArticleList grpcResponse = articleServiceClient.getArticles(builder.build());
        ArticleListResponse response = buildArticleListResponse(grpcResponse.getArticlesList());
        log.debug("Successfully retrieved {} articles out of {} total",
                response.getArticles().size(), grpcResponse.getArticlesCount());
        return response;
    }

    /**
     * Deletes an article if the requesting user is the owner.
     * @param articleId Article ID
     * @param requestingUserId User ID requesting deletion
     * @return Response indicating deletion status
     */
    public DeleteArticleResponse deleteArticle(Long articleId, Long requestingUserId) {
        log.debug("Deleting article with ID: {} for user: {}", articleId, requestingUserId);

        // First, get the article to check ownership
        ArticleProto.Article article = articleServiceClient.getArticle(articleId);

        // TODO: Send to article-service also the id of user request. The service itself should handle this.
        // Check if the requesting user is the owner
        if (article.getOwnerId() != requestingUserId) {
            log.warn("User {} attempted to delete article {} owned by user {}",
                    requestingUserId, articleId, article.getOwnerId());
            throw new ForbiddenAccessException("You have no access to delete this article");
        }

        // Delete the article via gRPC
        boolean success = articleServiceClient.deleteArticle(articleId);

        if (!success) {
            throw new RuntimeException("Failed to delete article");
        }

        log.debug("Successfully deleted article with ID: {}", articleId);

        return DeleteArticleResponse.builder()
                .message("Article deleted successfully")
                .build();

    }

    public void validateArticleExists(Long id) throws ArticleNotFoundException {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Article Id should be provided");
        }
        log.debug("Validating existence of article with ID: {}", id);
        ArticleProto.Article article = articleServiceClient.getArticle(id);
    }

    /**
     * Maps a gRPC Asset to an ArticleAssetResponse DTO.
     * @param asset gRPC asset object
     * @return Asset response DTO
     */
    public ArticleAssetResponse mapToAssetResponse(ArticleProto.Asset asset) {
        return ArticleAssetResponse.builder()
                .assetId(asset.getAssetId())
                .articleId(asset.getArticleId())
                .filename(asset.getFilename())
                .filesize(asset.getFilesize())
                .uploadDate(TimestampConverter.convertTimestampToIso(asset.getUploadDate()))
                .build();
    }

    private ArticleListResponse buildArticleListResponse(List<ArticleProto.Article> articles) {
        List<ArticleResponse> articleResponses = new ArrayList<>();
        HashMap<Long, UserResponse> authorCache = new HashMap<>();
        for (ArticleProto.Article article : articles) {
            // TODO: Implement batch loading with caching for better performance
            UserResponse author = authorCache.computeIfAbsent(article.getOwnerId(), userService::getUserByIdSafe);
            ArticleProto.Asset asset = articleServiceClient.getAssetByArticleId(article.getArticleId());
            articleResponses.add(buildArticleResponse(article, author, asset));
        }
        return ArticleListResponse.builder()
                .articles(articleResponses)
                .count(articleResponses.size())
                .build();
    }

    private ArticleResponse buildArticleResponse(ArticleProto.Article article, UserResponse author,
                                                 ArticleProto.Asset asset) {
        ArticleAssetResponse assetResponse = mapToAssetResponse(asset);
        return ArticleResponse.builder()
                .id(article.getArticleId())
                .title(article.getTitle())
                .shortDesc(article.getAbstract())
                .createdAt(TimestampConverter.convertTimestampToIso(article.getCreatedAt()))
                .views(article.getViews())
                .authorId(article.getOwnerId())
                .authorName(author.getName())
                .authorSurname(author.getSurname())
                .asset(assetResponse)
                .build();
    }
}
