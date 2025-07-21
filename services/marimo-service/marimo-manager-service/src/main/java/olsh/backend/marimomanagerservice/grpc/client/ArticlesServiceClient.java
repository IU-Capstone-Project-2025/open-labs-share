package olsh.backend.marimomanagerservice.grpc.client;

import olsh.backend.grpc.articles.ArticleServiceGrpc;
import olsh.backend.grpc.articles.GetArticleRequest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class ArticlesServiceClient {

    private final ManagedChannel channel;
    private final ArticleServiceGrpc.ArticleServiceBlockingStub blockingStub;

    public ArticlesServiceClient(
        @Value("${ARTICLES_SERVICE_HOST:articles-service}") String host,
        @Value("${ARTICLES_SERVICE_PORT:50051}") int port) {
        this.channel = ManagedChannelBuilder.forAddress(host, port)
            .usePlaintext()
            .build();

        this.blockingStub = ArticleServiceGrpc.newBlockingStub(channel);
        log.info("Initialized ArticlesServiceClient with connection to {}:{}", host, port);
    }

    public boolean articleExists(long articleId) {
        if (articleId <= 0) {
            return false;
        }
        log.debug("Checking existence of article with ID: {}", articleId);
        try {
            blockingStub.withDeadlineAfter(5, TimeUnit.SECONDS)
                .getArticle(GetArticleRequest.newBuilder().setArticleId(articleId).build());
            return true;
        } catch (Exception e) {
            log.warn("Article with ID {} not found or articles-service is down: {}", articleId, e.getMessage());
            return false;
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down ArticlesServiceClient");
        try {
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.warn("Error shutting down ArticlesServiceClient", e);
            Thread.currentThread().interrupt();
        }
    }
} 