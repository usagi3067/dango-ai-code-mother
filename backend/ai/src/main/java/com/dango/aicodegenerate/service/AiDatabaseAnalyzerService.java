package com.dango.aicodegenerate.service;

import com.dango.aicodegenerate.model.DatabaseAnalysisResult;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * AI 数据库分析服务接口
 * 用于分析用户需求，判断是否需要数据库操作
 * 配置了只读工具（文件读取），AI 可自行调用
 *
 * @author dango
 */
public interface AiDatabaseAnalyzerService {

    /**
     * 分析用户需求，输出 SQL 语句列表
     *
     * @param appId          应用 ID（用于记忆管理）
     * @param analysisRequest 分析请求（包含项目结构、数据库 Schema、用户需求）
     * @return 结构化的分析结果，包含 sqlStatements 列表
     */
    @SystemMessage(fromResource = "prompt/database-analyzer-system-prompt.txt")
    DatabaseAnalysisResult analyze(@MemoryId long appId, @UserMessage String analysisRequest);
}
