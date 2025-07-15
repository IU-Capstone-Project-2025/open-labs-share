package olsh.backend.marimoservice.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

@Configuration
public class GrpcClientConfig {

    @Bean
    @Qualifier("pythonMarimoServiceChannel")
    public ManagedChannel pythonMarimoServiceChannel(@Value("${grpc.client.python-marimo-service.address}") String address) {
        return ManagedChannelBuilder.forTarget(address)
                .usePlaintext()
                .build();
    }

    @Bean
    @Qualifier("authServiceChannel")
    public ManagedChannel authServiceChannel(@Value("${grpc.client.auth-service.address}") String address) {
        return ManagedChannelBuilder.forTarget(address)
                .usePlaintext()
                .build();
    }
    
    @Bean
    @Qualifier("usersServiceChannel")
    public ManagedChannel usersServiceChannel(@Value("${grpc.client.users-service.address}") String address) {
        return ManagedChannelBuilder.forTarget(address)
                .usePlaintext()
                .build();
    }

    @Bean
    @Qualifier("articlesServiceChannel")
    public ManagedChannel articlesServiceChannel(@Value("${grpc.client.articles-service.address}") String address) {
        return ManagedChannelBuilder.forTarget(address)
                .usePlaintext()
                .build();
    }

    @Bean
    @Qualifier("labsServiceChannel")
    public ManagedChannel labsServiceChannel(@Value("${grpc.client.labs-service.address}") String address) {
        return ManagedChannelBuilder.forTarget(address)
                .usePlaintext()
                .build();
    }
} 