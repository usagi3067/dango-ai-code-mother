package com.dango.dangoaicodeapp;

import dev.langchain4j.community.store.embedding.redis.spring.RedisEmbeddingStoreAutoConfiguration;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author dango
 * @description
 * @date
 */

@EnableScheduling
@EnableCaching
@SpringBootApplication(exclude = {RedisEmbeddingStoreAutoConfiguration.class, ElasticsearchRepositoriesAutoConfiguration.class, ElasticsearchDataAutoConfiguration.class, ElasticsearchRestClientAutoConfiguration.class})
@MapperScan("com.dango.dangoaicodeapp.infrastructure.mapper")
@ComponentScan(basePackages = {"com.dango.dangoaicodeapp", "com.dango.dangoaicodecommon", "com.dango.aicodegenerate"})
@EnableDubbo
public class DangoAiCodeAppApplication {
    public static void main(String[] args) {
        SpringApplication.run(DangoAiCodeAppApplication.class, args);
    }
}
