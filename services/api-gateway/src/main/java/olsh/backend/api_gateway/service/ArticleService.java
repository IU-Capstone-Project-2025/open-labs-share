package olsh.backend.api_gateway.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import olsh.backend.api_gateway.config.UploadFileConfiguration;
import olsh.backend.api_gateway.dto.request.CreateArticleRequest;
import olsh.backend.api_gateway.dto.request.GetArticlesRequest;
import olsh.backend.api_gateway.dto.response.*;
import olsh.backend.api_gateway.exception.ForbiddenAccessException;
import olsh.backend.api_gateway.grpc.client.ArticleServiceClient;
import olsh.backend.api_gateway.grpc.proto.ArticleProto;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleServiceClient articleServiceClient;
    private final UploadFileConfiguration uploadConfig;
    private final UserService userService;

    public CreateArticleResponse createArticle(CreateArticleRequest request, Long authorId) {
        log.debug("Creating article with title: {} for author: {}", request.getTitle(), authorId);

        validatePdfFile(request.getPdf_file());
        ArticleProto.Article article = registerArticle(request, authorId);
        articleServiceClient.uploadAsset(article.getArticleId(), request.getPdf_file());

        return CreateArticleResponse.builder().id(article.getArticleId()).message("Article created successfully").build();
    }

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

    public ArticleResponse getArticleById(Long articleId) {
        if (articleId == null || articleId == 0) {
            throw new IllegalArgumentException("ArticleId should be provided");
        }
        log.debug("Getting article with ID: {}", articleId);

        // Get article and its user from gRPC service
        ArticleProto.Article article = articleServiceClient.getArticle(articleId);
        UserResponse author = userService.getUserById(article.getOwnerId());

        log.debug("Successfully retrieved article: {}", article.getTitle());

        return buildArticleResponse(article, author);
    }

    public ArticleListResponse getArticlesByAuthor(long authorId, GetArticlesRequest request) {
        log.debug("Getting articles list for author {} - page: {}, limit: {}", authorId, request.getPage(), request.getLimit());

        // Step 1: Get the full user profile, which includes a list of their article IDs and titles.
        UserResponse authorWithContent = userService.getUserById(authorId);
        List<UserResponse.ArticleSummary> articleSummaries = authorWithContent.getArticles();

        if (articleSummaries == null || articleSummaries.isEmpty()) {
            return ArticleListResponse.builder()
                    .articles(new ArrayList<>())
                    .pagination(ArticleListResponse.PaginationResponse.builder()
                            .currentPage(1).totalPages(0).totalItems(0).build())
                    .build();
        }

        // Step 2: Paginate the list of article summaries manually.
        int totalItems = articleSummaries.size();
        int totalPages = (int) Math.ceil((double) totalItems / request.getLimit());
        int start = (request.getPage() - 1) * request.getLimit();
        int end = Math.min(start + request.getLimit(), totalItems);

        List<ArticleResponse> articleResponses = new ArrayList<>();
        if (start < end) {
            List<UserResponse.ArticleSummary> paginatedSummaries = articleSummaries.subList(start, end);

            // Step 3: Fetch full details for each article in the paginated list.
            for (UserResponse.ArticleSummary summary : paginatedSummaries) {
                // This will make a gRPC call for each article.
                // For high-traffic scenarios, a batch-get method would be more efficient.
                articleResponses.add(getArticleById(summary.getArticleId()));
            }
        }

        ArticleListResponse.PaginationResponse pagination =
                ArticleListResponse.PaginationResponse.builder()
                        .currentPage(request.getPage())
                        .totalPages(totalPages)
                        .totalItems(totalItems)
                        .build();

        return ArticleListResponse.builder()
                .articles(articleResponses)
                .pagination(pagination)
                .build();
    }

    public ArticleListResponse getArticles(GetArticlesRequest request) {
        log.debug("Getting articles list - page: {}, limit: {}", request.getPage(), request.getLimit());

        try {
            // Get articles from gRPC service
            ArticleProto.ArticleList grpcResponse =
                    articleServiceClient.getArticles(request.getPage(), request.getLimit());

            // Convert articles to response DTOs
            List<ArticleResponse> articleResponses = new ArrayList<>();

            HashMap<Long, UserResponse> authorCache = new HashMap<>();

            for (ArticleProto.Article article : grpcResponse.getArticlesList()) {
                // TODO: Implement batch loading with caching for better performance
                UserResponse author;
                if (authorCache.containsKey(article.getOwnerId())) {
                    author = authorCache.get(article.getOwnerId());
                } else {
                    author = userService.getUserById(article.getOwnerId());
                    authorCache.put(article.getOwnerId(), author);
                }

                articleResponses.add(buildArticleResponse(article, author));
            }

            // Calculate pagination
            int totalItems = (int) grpcResponse.getTotalCount();
            int totalPages = (int) Math.ceil((double) totalItems / request.getLimit());

            ArticleListResponse.PaginationResponse pagination =
                    ArticleListResponse.PaginationResponse.builder()
                            .currentPage(request.getPage())
                            .totalPages(totalPages)
                            .totalItems(totalItems)
                            .build();

            log.debug("Successfully retrieved {} articles out of {} total",
                    articleResponses.size(), totalItems);

            return ArticleListResponse.builder()
                    .articles(articleResponses)
                    .pagination(pagination)
                    .build();

        } catch (Exception e) {
            log.error("Error getting articles list: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve articles: " + e.getMessage());
        }
    }

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


    private ArticleResponse buildArticleResponse(ArticleProto.Article article, UserResponse author) {
        return ArticleResponse.builder()
                .id(article.getArticleId())
                .title(article.getTitle())
                .shortDesc(article.getAbstract())
                .createdAt(TimestampConverter.convertTimestampToIso(article.getCreatedAt()))
                .views(article.getViews())
                .authorId(article.getOwnerId())
                .authorName(author.getName())
                .authorSurname(author.getSurname())
                .build();
    }
}
