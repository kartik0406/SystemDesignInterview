package com.sdi.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    @Value("${app.llm-service.url}")
    private String llmServiceUrl;

    @Value("${app.interview.max-rounds}")
    private int maxRounds;

    @Value("${app.interview.session-ttl-minutes}")
    private int sessionTtlMinutes;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    public String getLlmServiceUrl() {
        return llmServiceUrl;
    }

    public int getMaxRounds() {
        return maxRounds;
    }

    public int getSessionTtlMinutes() {
        return sessionTtlMinutes;
    }
}
