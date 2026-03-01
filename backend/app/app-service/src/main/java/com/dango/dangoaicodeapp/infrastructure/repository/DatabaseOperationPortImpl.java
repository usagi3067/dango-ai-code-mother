package com.dango.dangoaicodeapp.infrastructure.repository;

import com.dango.dangoaicodeapp.domain.codegen.port.DatabaseOperationPort;
import com.dango.supabase.dto.TableSchemaDTO;
import com.dango.supabase.service.SupabaseService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 数据库操作端口适配器。
 */
@Slf4j
@Component
public class DatabaseOperationPortImpl implements DatabaseOperationPort {

    @DubboReference
    private SupabaseService supabaseService;

    @Override
    public String executeSql(Long appId, String sql) {
        return supabaseService.executeSql(appId, sql);
    }

    @Override
    public String getFormattedSchema(Long appId) {
        List<TableSchemaDTO> latestSchema = supabaseService.getSchema(appId);
        if (latestSchema == null || latestSchema.isEmpty()) {
            return "无表";
        }
        return latestSchema.stream()
                .collect(Collectors.groupingBy(TableSchemaDTO::getTableName))
                .entrySet().stream()
                .map(entry -> {
                    String tableName = entry.getKey();
                    List<TableSchemaDTO> columns = entry.getValue();
                    String columnStr = columns.stream()
                            .map(col -> String.format("  - %s: %s%s",
                                    col.getColumnName(),
                                    col.getDataType(),
                                    Boolean.TRUE.equals(col.getIsNullable()) ? "" : " NOT NULL"))
                            .collect(Collectors.joining("\n"));
                    return String.format("表 %s:\n%s", tableName, columnStr);
                })
                .collect(Collectors.joining("\n\n"));
    }
}
