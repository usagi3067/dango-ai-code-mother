package com.dango.supabase.service.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.dango.supabase.client.SupabaseManagementClient;
import com.dango.supabase.config.SupabaseProperties;
import com.dango.supabase.dto.SupabaseConfigDTO;
import com.dango.supabase.dto.TableSchemaDTO;
import com.dango.supabase.dto.TableSummaryDTO;
import com.dango.supabase.service.SupabaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Supabase 服务实现
 *
 * @author dango
 */
@Slf4j
@Service
@DubboService
@RequiredArgsConstructor
public class SupabaseServiceImpl implements SupabaseService {

    private final SupabaseManagementClient managementClient;
    private final SupabaseProperties properties;

    @Override
    public SupabaseConfigDTO createSchema(Long appId) {
        String schemaName = properties.getSchemaName(appId);
        log.info("创建 Schema: {}", schemaName);

        // 1. 创建 Schema
        String createSchemaSql = String.format("CREATE SCHEMA IF NOT EXISTS %s", schemaName);
        managementClient.executeSql(createSchemaSql);

        // 2. 授权 anon 角色访问 Schema
        String grantSql = String.format("""
                GRANT USAGE ON SCHEMA %s TO anon;
                ALTER DEFAULT PRIVILEGES IN SCHEMA %s
                GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO anon
                """, schemaName, schemaName);
        managementClient.executeSql(grantSql);

        // 3. 暴露 Schema 到 REST API
        managementClient.exposeSchema(schemaName);

        log.info("Schema {} 创建成功", schemaName);

        return SupabaseConfigDTO.builder()
                .url(properties.getUrl())
                .anonKey(properties.getAnonKey())
                .schemaName(schemaName)
                .build();
    }

    @Override
    public void deleteSchema(Long appId) {
        String schemaName = properties.getSchemaName(appId);
        log.info("删除 Schema: {}", schemaName);

        // 1. 从 REST API 移除 Schema
        managementClient.unexposeSchema(schemaName);

        // 2. 删除 Schema（CASCADE 会删除所有表）
        String dropSchemaSql = String.format("DROP SCHEMA IF EXISTS %s CASCADE", schemaName);
        managementClient.executeSql(dropSchemaSql);

        log.info("Schema {} 删除成功", schemaName);
    }

    @Override
    public List<TableSchemaDTO> getSchema(Long appId) {
        String schemaName = properties.getSchemaName(appId);

        // 查询 information_schema 获取表结构
        String sql = String.format("""
                SELECT
                    c.table_name,
                    c.column_name,
                    c.data_type,
                    c.is_nullable = 'YES' as is_nullable,
                    c.column_default,
                    CASE WHEN pk.column_name IS NOT NULL THEN true ELSE false END as is_primary_key
                FROM information_schema.columns c
                LEFT JOIN (
                    SELECT kcu.column_name, kcu.table_name
                    FROM information_schema.table_constraints tc
                    JOIN information_schema.key_column_usage kcu
                        ON tc.constraint_name = kcu.constraint_name
                        AND tc.table_schema = kcu.table_schema
                    WHERE tc.constraint_type = 'PRIMARY KEY'
                    AND tc.table_schema = '%s'
                ) pk ON c.column_name = pk.column_name AND c.table_name = pk.table_name
                WHERE c.table_schema = '%s'
                ORDER BY c.table_name, c.ordinal_position
                """, schemaName, schemaName);

        String result = managementClient.executeSql(sql);
        JSONArray rows = JSONUtil.parseArray(result);

        List<TableSchemaDTO> schemas = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            JSONObject row = rows.getJSONObject(i);
            schemas.add(TableSchemaDTO.builder()
                    .tableName(row.getStr("table_name"))
                    .columnName(row.getStr("column_name"))
                    .dataType(row.getStr("data_type"))
                    .isNullable(row.getBool("is_nullable"))
                    .columnDefault(row.getStr("column_default"))
                    .isPrimaryKey(row.getBool("is_primary_key"))
                    .build());
        }

        return schemas;
    }

    @Override
    public List<TableSummaryDTO> getTableSummary(Long appId) {
        String schemaName = properties.getSchemaName(appId);

        // 获取所有表名
        String tablesSql = String.format("""
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = '%s'
                AND table_type = 'BASE TABLE'
                """, schemaName);

        String tablesResult = managementClient.executeSql(tablesSql);
        JSONArray tables = JSONUtil.parseArray(tablesResult);

        List<TableSummaryDTO> summaries = new ArrayList<>();
        for (int i = 0; i < tables.size(); i++) {
            String tableName = tables.getJSONObject(i).getStr("table_name");

            // 获取每个表的行数
            String countSql = String.format("SELECT COUNT(*) as count FROM %s.%s", schemaName, tableName);
            String countResult = managementClient.executeSql(countSql);
            JSONArray countRows = JSONUtil.parseArray(countResult);
            Long rowCount = countRows.isEmpty() ? 0L : countRows.getJSONObject(0).getLong("count");

            summaries.add(TableSummaryDTO.builder()
                    .tableName(tableName)
                    .rowCount(rowCount)
                    .build());
        }

        return summaries;
    }

    @Override
    public String executeSql(Long appId, String sql) {
        String schemaName = properties.getSchemaName(appId);

        // 设置 search_path，使 SQL 中的表名自动解析到应用 Schema
        String fullSql = String.format("SET search_path TO %s; %s", schemaName, sql);

        return managementClient.executeSql(fullSql);
    }

    @Override
    public SupabaseConfigDTO getSupabaseConfig(Long appId) {
        String schemaName = properties.getSchemaName(appId);
        return SupabaseConfigDTO.builder()
                .url(properties.getUrl())
                .anonKey(properties.getAnonKey())
                .schemaName(schemaName)
                .build();
    }
}
