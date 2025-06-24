package olsh.backend.api_gateway.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import olsh.backend.api_gateway.annotation.RequireAuth;
import olsh.backend.api_gateway.dto.request.CreateArticleRequest;
import olsh.backend.api_gateway.dto.request.GetArticlesRequest;
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

@Slf4j
@RestController
@RequestMapping("/api/v1/articles")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ArticleController {

    private final ArticleService articleService;

    @Autowired
    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @RequireAuth
    @PostMapping
    public ResponseEntity<CreateArticleResponse> createArticle(
            @Valid @ModelAttribute CreateArticleRequest request,
            HttpServletRequest httpRequest) {

        log.debug("Received request to create article with title: {}", request.getTitle());

        CreateArticleResponse response = articleService.createArticle(request, extractUserIdFromRequest(httpRequest));

        log.debug("Successfully created article");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @RequireAuth
    @GetMapping("/{article_id}")
    public ResponseEntity<ArticleResponse> getArticle(
            @PathVariable("article_id") Long articleId,
            HttpServletRequest request) {

        log.debug("Received request to get article with ID: {}", articleId);
        ArticleResponse response = articleService.getArticleById(articleId);

        log.debug("Successfully retrieved article data for articleId: {}", articleId);
        return ResponseEntity.ok(response);
    }

    @RequireAuth
    @GetMapping
    public ResponseEntity<ArticleListResponse> getArticles(
            @Valid @ParameterObject GetArticlesRequest request,
            HttpServletRequest httpRequest) {

        log.debug("Received request to get articles with page: {}, limit: {}", request.getPage(), request.getLimit());

        ArticleListResponse response = articleService.getArticles(request);

        log.debug("Successfully retrieved articles list with {} articles", response.getArticles().size());
        return ResponseEntity.ok(response);
    }

    @RequireAuth
    @DeleteMapping("/{article_id}")
    public ResponseEntity<DeleteArticleResponse> deleteArticle(
            @PathVariable("article_id") Long articleId,
            HttpServletRequest request) {

        log.debug("Received request to delete article with ID: {}", articleId);

        Long userId = extractUserIdFromRequest(request);
        DeleteArticleResponse response = articleService.deleteArticle(articleId, userId);

        log.debug("Successfully deleted article with ID: {}", articleId);
        return ResponseEntity.ok(response);
    }


    private Long extractUserIdFromRequest(HttpServletRequest request) {
        try {
            // Get the authenticated user info stored by AuthInterceptor
            olsh.backend.api_gateway.grpc.model.UserInfo userInfo =
                    (olsh.backend.api_gateway.grpc.model.UserInfo) request.getAttribute("authenticatedUser");

            if (userInfo == null) {
                log.error("No authenticated user found in request attributes");
                throw new RuntimeException("Authentication information not found");
            }

            return userInfo.getId();

        } catch (Exception e) {
            log.error("Failed to extract user ID from request: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to extract user information", e);
        }
    }
}

