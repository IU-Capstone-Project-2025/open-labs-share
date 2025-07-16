package olsh.backend.api_gateway.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import olsh.backend.api_gateway.annotation.RequireAuth;
import olsh.backend.api_gateway.dto.request.CreateArticleRequest;
import olsh.backend.api_gateway.dto.request.ArticlesGetRequest;
import olsh.backend.api_gateway.dto.response.ArticleListResponse;
import olsh.backend.api_gateway.dto.response.ArticleResponse;
import olsh.backend.api_gateway.dto.response.CreateArticleResponse;
import olsh.backend.api_gateway.dto.response.DeleteArticleResponse;
import olsh.backend.api_gateway.service.ArticleService;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@Slf4j
@RestController
@RequestMapping("/api/v1/articles")
@CrossOrigin(origins = {"http://localhost:5173", "https://open-labs-share.online"}, allowCredentials = "true", maxAge = 3600)
@Tag(name = "Articles", description = "Endpoints for managing articles in PDF format")
@SecurityRequirement(name = "bearerAuth")
public class ArticleController {

    private final ArticleService articleService;
    private final RequestAttributesExtractor attributesProvider;

    @Autowired
    public ArticleController(ArticleService articleService, RequestAttributesExtractor attributesProvider) {
        this.articleService = articleService;
        this.attributesProvider = attributesProvider;
    }

    @Operation(
            summary = "Create new article",
            description = "Creates a new article with PDF file upload. Requires authentication."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Article created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation =
                            CreateArticleResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Access denied")
    })
    @RequireAuth
    @PostMapping
    public ResponseEntity<CreateArticleResponse> createArticle(
            @Valid @ModelAttribute @Parameter(description = "Article creation data including title, description, and " +
                    "PDF file") CreateArticleRequest request,
            HttpServletRequest httpRequest) {

        log.debug("Received request to create article with title: {}", request.getTitle());

        CreateArticleResponse response = articleService.createArticle(request,
                attributesProvider.extractUserIdFromRequest(httpRequest));

        log.debug("Successfully created article");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Get article by ID",
            description = "Retrieves detailed information about a specific article by its ID. Requires authentication."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Article found and returned successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation =
                            ArticleResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
            @ApiResponse(responseCode = "404", description = "Article not found")
    })
    @RequireAuth
    @GetMapping("/{article_id}")
    public ResponseEntity<ArticleResponse> getArticle(
            @Parameter(description = "ID of the article to retrieve", required = true)
            @PathVariable("article_id") Long articleId,
            HttpServletRequest request) {

        log.debug("Received request to get article with ID: {}", articleId);
        ArticleResponse response = articleService.getArticleById(articleId);

        log.debug("Successfully retrieved article data for articleId: {}", articleId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get list of articles",
            description = "Retrieves a paginated list of articles. Requires authentication."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Articles retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation =
                            ArticleListResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required")
    })
    @RequireAuth
    @GetMapping
    public ResponseEntity<ArticleListResponse> getArticles(
            @Valid @ParameterObject @Parameter(description = "Pagination parameters") ArticlesGetRequest request,
            HttpServletRequest httpRequest) {

        log.debug("Received request to get articles with page: {}, limit: {}", request.getPage(), request.getLimit());

        ArticleListResponse response = articleService.getArticles(request);

        log.debug("Successfully retrieved articles list with {} articles", response.getArticles().size());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get list of articles for the current user",
            description = "Retrieves a paginated list of articles for the currently authenticated user."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User's articles retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation =
                            ArticleListResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required")
    })
    @RequireAuth
    @GetMapping("/my")
    public ResponseEntity<ArticleListResponse> getMyArticles(
            HttpServletRequest httpRequest,
            @RequestParam(defaultValue = "1") @Parameter(description = "Page number (starts from 1)", example = "1") Integer page,
            @RequestParam(defaultValue = "20") @Parameter(description = "Page size", example = "20") Integer limit) {

        long userId = attributesProvider.extractUserIdFromRequest(httpRequest);
        log.debug("Received request to get articles for user: {}", userId);

        ArticleListResponse response = articleService.getUsersArticles(userId, page, limit);

        log.debug("Successfully retrieved {} articles for user {}", response.getArticles().size(), userId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Delete article",
            description = "Deletes a specific article by its ID. Only the article owner can delete it. Requires " +
                    "authentication."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Article deleted successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation =
                            DeleteArticleResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - No access to delete the article"),
            @ApiResponse(responseCode = "404", description = "Article not found")
    })
    @RequireAuth
    @DeleteMapping("/{article_id}")
    public ResponseEntity<DeleteArticleResponse> deleteArticle(
            @Parameter(description = "ID of the article to delete", required = true)
            @PathVariable("article_id") Long articleId,
            HttpServletRequest request) {

        log.debug("Received request to delete article with ID: {}", articleId);

        Long userId = attributesProvider.extractUserIdFromRequest(request);
        DeleteArticleResponse response = articleService.deleteArticle(articleId, userId);

        log.debug("Successfully deleted article with ID: {}", articleId);
        return ResponseEntity.ok(response);
    }
}

