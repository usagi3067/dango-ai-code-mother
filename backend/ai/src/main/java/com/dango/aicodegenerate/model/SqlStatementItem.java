package com.dango.aicodegenerate.model;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SQL 语句项
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SqlStatementItem {
    @JsonPropertyDescription("SQL 类型：DDL（表结构变更）、DML（数据变更）、DQL（数据查询）")
    private String type;

    @JsonPropertyDescription("SQL 语句，不要包含 Schema 名，执行时会自动添加")
    private String sql;

    @JsonPropertyDescription("SQL 说明，描述这条 SQL 的作用")
    private String description;
}
