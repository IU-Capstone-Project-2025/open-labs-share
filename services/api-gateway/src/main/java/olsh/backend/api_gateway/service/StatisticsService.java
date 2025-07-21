package olsh.backend.api_gateway.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import olsh.backend.api_gateway.dto.response.StatisticsResponse;
import olsh.backend.api_gateway.grpc.client.ArticleServiceClient;
import olsh.backend.api_gateway.grpc.client.LabServiceClient;
import olsh.backend.api_gateway.grpc.client.SubmissionServiceClient;
import olsh.backend.api_gateway.grpc.client.UserServiceClient;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Slf4j
@Service
public class StatisticsService {

    private final UserServiceClient userServiceClient;
    private final LabServiceClient labServiceClient;
    private final ArticleServiceClient articleServiceClient;
    private final SubmissionServiceClient submissionServiceClient;


    public StatisticsResponse getStatistics() {
        log.info("Fetching statistics from all services");
        Integer usersCount = userServiceClient.usersCount();
        Integer labsCount = labServiceClient.labCount();
        Integer articlesCount = articleServiceClient.articlesCount();
        Integer submissionsCount = submissionServiceClient.submissionsCount();
        StatisticsResponse response = StatisticsResponse.builder()
                .users(usersCount)
                .labs(labsCount)
                .articles(articlesCount)
                .submissions(submissionsCount)
                .build();
        log.debug("Statistics response: {}", response);
        return response;
    }
}
