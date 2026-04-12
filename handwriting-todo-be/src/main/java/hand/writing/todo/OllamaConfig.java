package hand.writing.todo;

import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ReactorClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Configuration
public class OllamaConfig {

    @Value("${vast.api.token:}")
    private String vastToken;

    @Value("${spring.ai.ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

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

    @Bean
    public OllamaApi ollamaApi(WebClient.Builder webClientBuilder) {
        var requestFactory = new ReactorClientHttpRequestFactory();
        requestFactory.setReadTimeout(Duration.ofSeconds(360));

        return OllamaApi.builder()
                .baseUrl(ollamaBaseUrl)
                .restClientBuilder(RestClient.builder().requestFactory(requestFactory))
                .webClientBuilder(webClientBuilder)
                .build();
    }
}
