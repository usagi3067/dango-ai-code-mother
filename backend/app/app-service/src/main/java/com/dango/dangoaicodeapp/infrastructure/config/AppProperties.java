package com.dango.dangoaicodeapp.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String previewHost = "http://localhost:8124";

    private String deployHost = "http://localhost";
}
