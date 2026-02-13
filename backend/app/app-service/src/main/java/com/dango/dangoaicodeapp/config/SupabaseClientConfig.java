package com.dango.dangoaicodeapp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Supabase 客户端配置
 * 用于生成前端 Supabase 客户端配置文件
 *
 * @author dango
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "supabase.client")
public class SupabaseClientConfig {

    /**
     * Supabase 服务 URL
     */
    private String url;

    /**
     * Supabase 匿名密钥（用于前端访问）
     */
    private String anonKey;
}
