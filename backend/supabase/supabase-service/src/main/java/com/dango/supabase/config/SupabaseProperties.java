package com.dango.supabase.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Supabase 配置
 *
 * @author dango
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "supabase")
public class SupabaseProperties {

    /**
     * Supabase 项目 URL（前端使用）
     */
    private String url;

    /**
     * Anon Key（前端使用，可公开）
     */
    private String anonKey;

    /**
     * Management API Token（后端使用，需保密）
     */
    private String managementToken;

    /**
     * 项目 Reference ID
     */
    private String projectRef;

    /**
     * Management API Base URL
     */
    private String managementApiUrl = "https://api.supabase.com/v1/projects";

    /**
     * 获取 Schema 名称
     */
    public String getSchemaName(Long appId) {
        return "app_" + appId;
    }
}
