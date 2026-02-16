package com.dango.dangoaicodeapp.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

/**
 * ES 条件配置：仅当 search.es.enabled=true 时启用 ES 客户端和 Repository 扫描
 */
@Configuration
@ConditionalOnProperty(name = "search.es.enabled", havingValue = "true")
@EnableElasticsearchRepositories(basePackages = "com.dango.dangoaicodeapp.esdao")
@Import({ElasticsearchRestClientAutoConfiguration.class, ElasticsearchDataAutoConfiguration.class})
public class ElasticsearchConfig {
}
