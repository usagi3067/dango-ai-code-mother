package com.dango.dangoaicodeapp.domain.codegen.port;

/**
 * 数据库操作端口。
 * 屏蔽 Dubbo/Supabase 细节。
 */
public interface DatabaseOperationPort {

    String executeSql(Long appId, String sql);

    String getFormattedSchema(Long appId);
}
