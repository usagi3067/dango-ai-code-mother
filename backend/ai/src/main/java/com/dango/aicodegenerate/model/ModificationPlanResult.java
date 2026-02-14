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
 * 修改规划结果
 * ModificationPlannerNode 的输出
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModificationPlanResult implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Description("规划说明，简要描述分析结果")
    private String analysis;

    @Description("修改策略，说明整体修改思路")
    private String strategy;

    @Description("SQL 语句列表，如果不需要数据库操作则为空数组")
    private List<SqlStatementItem> sqlStatements;

    @Description("需要修改的文件列表，包含路径、类型和具体操作")
    private List<FileModificationGuide> filesToModify;
}
