package com.dango.aicodegenerate.service;

import com.dango.aicodegenerate.model.ModificationPlanResult;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * AI 修改规划服务接口
 * 负责分析用户需求，输出数据库操作计划和代码修改计划
 * 配置了只读工具（文件读取），AI 可自行调用
 *
 * @author dango
 */
public interface AiModificationPlannerService {

    /**
     * 分析用户需求，输出完整的修改规划
     *
     * @param appId          应用 ID（用于记忆管理）
     * @param planningRequest 规划请求（包含项目结构、数据库 Schema、用户需求）
     * @return 结构化的修改规划结果
     */
    @SystemMessage(fromResource = "prompt/modification-planner-system-prompt.txt")
    ModificationPlanResult plan(@MemoryId long appId, @UserMessage String planningRequest);
}
