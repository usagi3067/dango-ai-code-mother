package com.dango.supabase.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 表结构信息 DTO
 *
 * @author dango
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableSchemaDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 表名
     */
    private String tableName;

    /**
     * 列名
     */
    private String columnName;

    /**
     * 数据类型
     */
    private String dataType;

    /**
     * 是否可为空
     */
    private Boolean isNullable;

    /**
     * 默认值
     */
    private String columnDefault;

    /**
     * 是否为主键
     */
    private Boolean isPrimaryKey;
}
