package com.dango.dangoaicodeapp.workflow.state;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * SQL 执行结果
 *
 * @author dango
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SqlExecutionResult implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 执行的 SQL
     */
    private String sql;

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 影响行数（DML）或执行消息
     */
    private String result;

    /**
     * 错误信息（如果失败）
     */
    private String error;
}
