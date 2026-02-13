package com.dango.aicodegenerate.model;

import dev.langchain4j.model.output.structured.Description;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 数据库分析结果
 *
 * @author dango
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Description("数据库分析结果，包含分析说明和待执行的 SQL 语句列表")
public class DatabaseAnalysisResult implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 分析说明
     */
    @Description("简要说明分析结果，解释为什么需要或不需要数据库操作")
    private String analysis;

    /**
     * SQL 语句列表
     */
    @Description("待执行的 SQL 语句列表，如果不需要数据库操作则为空列表")
    private List<SqlStatementItem> sqlStatements;

    /**
     * SQL 语句项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Description("单条 SQL 语句")
    public static class SqlStatementItem implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * SQL 类型
         */
        @Description("SQL 类型：DDL（CREATE/ALTER/DROP TABLE）、DML（INSERT/UPDATE/DELETE）、DQL（SELECT）")
        private String type;

        /**
         * SQL 语句
         */
        @Description("完整的 SQL 语句，不要包含 Schema 名前缀")
        private String sql;

        /**
         * 说明
         */
        @Description("这条 SQL 的作用说明")
        private String description;
    }
}
