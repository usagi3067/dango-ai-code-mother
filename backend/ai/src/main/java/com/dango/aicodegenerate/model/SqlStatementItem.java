package com.dango.aicodegenerate.model;

import dev.langchain4j.model.output.structured.Description;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * SQL 语句项
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SqlStatementItem implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Description("SQL 类型：DDL（CREATE/ALTER/DROP TABLE）、DML（INSERT/UPDATE/DELETE）、DQL（SELECT）")
    private String type;

    @Description("完整的 SQL 语句，不要包含 Schema 名前缀")
    private String sql;

    @Description("这条 SQL 的作用说明")
    private String description;
}
