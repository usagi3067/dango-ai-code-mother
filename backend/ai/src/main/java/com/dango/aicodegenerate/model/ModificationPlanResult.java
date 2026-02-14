package com.dango.aicodegenerate.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 修改规划结果
 * ModificationPlannerNode 的输出
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModificationPlanResult {

    @JsonPropertyDescription("规划说明，简要描述分析结果")
    private String analysis;

    @JsonPropertyDescription("修改策略，说明整体修改思路")
    private String strategy;

    @JsonPropertyDescription("SQL 语句列表，如果不需要数据库操作则为空数组")
    @JsonProperty("sqlStatements")
    private List<SqlStatementItem> sqlStatements;

    @JsonPropertyDescription("需要修改的文件列表，包含路径、类型和具体操作")
    @JsonProperty("filesToModify")
    private List<FileModificationGuide> filesToModify;
}
