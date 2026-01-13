package luti.server.infrastructure.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

import luti.server.infrastructure.client.kgs.KgsErrorHandler;

@Configuration
public class RestTemplateConfig {

    private final KgsErrorHandler kgsErrorHandler;

    public RestTemplateConfig(KgsErrorHandler kgsErrorHandler) {
        this.kgsErrorHandler = kgsErrorHandler;
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
            .setConnectTimeout(Duration.ofSeconds(5))
            .setReadTimeout(Duration.ofSeconds(5))
            .build();
    }
}
