package com.dango.aicodegenerate.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "ai")
@Data
public class AiGatewayProperties {

    private Gateway gateway = new Gateway();
    private String defaultModel = "cheap-model";
    private Map<String, ServiceConfig> services = new HashMap<>();

    @Data
    public static class Gateway {
        private String baseUrl;
        private String apiKey;
        private Integer defaultMaxTokens = 8192;
        private Duration defaultTimeout = Duration.ofSeconds(60);
        private Boolean logRequests = false;
        private Boolean logResponses = false;
    }

    @Data
    public static class ServiceConfig {
        private String model;
        private Integer maxTokens;
        private Duration timeout;
    }
}
