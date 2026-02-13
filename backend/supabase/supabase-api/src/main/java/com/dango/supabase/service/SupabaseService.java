package com.dango.supabase.service;

import com.dango.supabase.dto.SupabaseConfigDTO;
import com.dango.supabase.dto.TableSchemaDTO;
import com.dango.supabase.dto.TableSummaryDTO;

import java.util.List;

/**
 * Supabase 服务接口（Dubbo RPC）
 *
 * @author dango
 */
public interface SupabaseService {

    /**
     * 为应用创建独立 Schema
     * 包含：创建 Schema + 授权 anon 角色 + 暴露到 REST API
     *
     * @param appId 应用 ID
     * @return Supabase 配置信息（url、anonKey、schemaName）
     */
    SupabaseConfigDTO createSchema(Long appId);

    /**
     * 删除应用的 Schema（应用删除时调用）
     *
     * @param appId 应用 ID
     */
    void deleteSchema(Long appId);

    /**
     * 获取应用的数据库 Schema 信息（表结构）
     *
     * @param appId 应用 ID
     * @return 表结构列表
     */
    List<TableSchemaDTO> getSchema(Long appId);

    /**
     * 获取表摘要（表名 + 行数，用于前端展示）
     *
     * @param appId 应用 ID
     * @return 表摘要列表
     */
    List<TableSummaryDTO> getTableSummary(Long appId);

    /**
     * 执行 SQL（供 AI 工作流调用）
     * 自动为表名添加 Schema 前缀
     *
     * @param appId 应用 ID
     * @param sql   SQL 语句
     * @return 执行结果（JSON 格式）
     */
    String executeSql(Long appId, String sql);

    /**
     * 获取 Supabase 配置信息
     *
     * @param appId 应用 ID
     * @return Supabase 配置信息
     */
    SupabaseConfigDTO getSupabaseConfig(Long appId);
}
