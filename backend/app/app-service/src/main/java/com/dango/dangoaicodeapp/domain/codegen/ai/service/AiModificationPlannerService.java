package com.dango.dangoaicodeapp.domain.codegen.ai.service;

import com.dango.aicodegenerate.model.ModificationPlanResult;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
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
     * 分析用户需求，输出完整的修改规划（同步方式）
     *
     * @param appId          应用 ID（用于记忆管理）
     * @param planningRequest 规划请求（包含项目结构、数据库 Schema、用户需求）
     * @return 结构化的修改规划结果
     */
    @SystemMessage(fromResource = "prompt/modification-planner-system-prompt.txt")
    ModificationPlanResult plan(@MemoryId long appId, @UserMessage String planningRequest);

    /**
     * 分析用户需求，输出完整的修改规划（流式方式）
     * 用于实时输出 AI 的思考过程，但最终仍需调用 plan() 获取结构化结果
     *
     * @param appId          应用 ID（用于记忆管理）
     * @param planningRequest 规划请求（包含项目结构、数据库 Schema、用户需求）
     * @return TokenStream 流式输出
     */
    @SystemMessage(fromResource = "prompt/modification-planner-system-prompt.txt")
    TokenStream planStream(@MemoryId long appId, @UserMessage String planningRequest);
}
