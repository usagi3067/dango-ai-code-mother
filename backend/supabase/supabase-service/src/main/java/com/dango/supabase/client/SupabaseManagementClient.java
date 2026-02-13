package com.dango.supabase.client;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.dango.supabase.config.SupabaseProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;

/**
 * Supabase Management API 客户端
 * 封装对 Supabase Management API 的调用
 *
 * @author dango
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SupabaseManagementClient {

    private final SupabaseProperties properties;

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    /**
     * 执行 SQL 语句
     *
     * @param sql SQL 语句
     * @return 执行结果（JSON 格式）
     */
    public String executeSql(String sql) {
        String url = String.format("%s/%s/database/query",
                properties.getManagementApiUrl(), properties.getProjectRef());

        JSONObject body = new JSONObject();
        body.set("query", sql);

        log.info("执行 SQL: {}", sql);

        HttpResponse response = HttpRequest.post(url)
                .header("Authorization", "Bearer " + properties.getManagementToken())
                .header("Content-Type", "application/json")
                .body(body.toString())
                .timeout(30000)
                .execute();

        String result = response.body();
        log.info("SQL 执行结果: {}", result);

        if (!response.isOk()) {
            log.error("SQL 执行失败: status={}, body={}", response.getStatus(), result);
            throw new RuntimeException("SQL 执行失败: " + result);
        }

        return result;
    }

    /**
     * 获取当前暴露的 Schema 列表
     *
     * @return db_schema 配置值
     */
    public String getExposedSchemas() {
        String url = String.format("%s/%s/postgrest",
                properties.getManagementApiUrl(), properties.getProjectRef());

        HttpResponse response = HttpRequest.get(url)
                .header("Authorization", "Bearer " + properties.getManagementToken())
                .timeout(30000)
                .execute();

        if (!response.isOk()) {
            log.error("获取 PostgREST 配置失败: {}", response.body());
            throw new RuntimeException("获取 PostgREST 配置失败");
        }

        JSONObject config = JSONUtil.parseObj(response.body());
        return config.getStr("db_schema");
    }

    /**
     * 更新暴露的 Schema 列表
     *
     * @param dbSchema 新的 db_schema 配置值
     */
    public void updateExposedSchemas(String dbSchema) {
        String url = String.format("%s/%s/postgrest",
                properties.getManagementApiUrl(), properties.getProjectRef());

        JSONObject body = new JSONObject();
        body.set("db_schema", dbSchema);

        log.info("更新暴露的 Schema: {}", dbSchema);

        try {
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + properties.getManagementToken())
                    .header("Content-Type", "application/json")
                    .method("PATCH", java.net.http.HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            java.net.http.HttpResponse<String> response = HTTP_CLIENT.send(request,
                    java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                log.error("更新 PostgREST 配置失败: {}", response.body());
                throw new RuntimeException("更新 PostgREST 配置失败");
            }

            log.info("Schema 暴露成功");
        } catch (Exception e) {
            log.error("更新 PostgREST 配置异常", e);
            throw new RuntimeException("更新 PostgREST 配置失败", e);
        }
    }

    /**
     * 暴露新的 Schema 到 REST API
     *
     * @param schemaName Schema 名称
     */
    public void exposeSchema(String schemaName) {
        String currentSchemas = getExposedSchemas();

        // 检查是否已经暴露
        if (currentSchemas.contains(schemaName)) {
            log.info("Schema {} 已经暴露，跳过", schemaName);
            return;
        }

        // 添加新 Schema
        String newSchemas = currentSchemas + ", " + schemaName;
        updateExposedSchemas(newSchemas);
    }

    /**
     * 从 REST API 移除 Schema
     *
     * @param schemaName Schema 名称
     */
    public void unexposeSchema(String schemaName) {
        String currentSchemas = getExposedSchemas();

        // 移除指定 Schema
        String newSchemas = currentSchemas
                .replace(", " + schemaName, "")
                .replace(schemaName + ", ", "");

        if (!newSchemas.equals(currentSchemas)) {
            updateExposedSchemas(newSchemas);
        }
    }
}
