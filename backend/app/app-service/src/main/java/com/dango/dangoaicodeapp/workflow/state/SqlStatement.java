package com.dango.dangoaicodeapp.workflow.state;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * SQL 语句封装
 *
 * @author dango
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SqlStatement implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * SQL 类型：DDL / DML / DQL
     */
    private String type;

    /**
     * SQL 语句
     */
    private String sql;

    /**
     * 说明（用于日志和调试）
     */
    private String description;
}
