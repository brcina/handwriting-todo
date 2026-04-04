package hand.writing.todo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class OllamaConfig {
    @Value("${vast.api.token:}")
    private String vastToken;

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
                .filter((request, next) -> {
                    if (!vastToken.isBlank()) {
                        ClientRequest withAuth = ClientRequest.from(request)
                                .header("Authorization", "Bearer " + vastToken)
                                .build();
                        return next.exchange(withAuth);
                    }
                    return next.exchange(request);
                });
    }
}
