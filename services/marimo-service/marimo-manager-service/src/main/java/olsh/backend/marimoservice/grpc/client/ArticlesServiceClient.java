package olsh.backend.marimoservice.grpc.client;

import com.olsh.articles.proto.ArticleServiceGrpc;
import com.olsh.articles.proto.GetArticleRequest;
import io.grpc.ManagedChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ArticlesServiceClient {

    private final ArticleServiceGrpc.ArticleServiceBlockingStub blockingStub;

    public ArticlesServiceClient(@Qualifier("articlesServiceChannel") ManagedChannel articlesServiceChannel) {
        this.blockingStub = ArticleServiceGrpc.newBlockingStub(articlesServiceChannel);
    }

    public boolean articleExists(long articleId) {
        if (articleId <= 0) {
            return false;
        }
        log.debug("Checking existence of article with ID: {}", articleId);
        try {
            blockingStub.getArticle(GetArticleRequest.newBuilder().setArticleId(articleId).build());
            return true;
        } catch (Exception e) {
            log.warn("Article with ID {} not found or articles-service is down: {}", articleId, e.getMessage());
            return false;
        }
    }
} 