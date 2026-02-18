package com.dango.dangoaicodeapp.domain.codegen.ai.service;

import com.dango.aicodegenerate.model.QualityResult;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * 代码质量检查 AI 服务接口
 * 使用 LangChain4j 框架，通过 AI 分析代码质量
 */
public interface CodeQualityCheckService {

    /**
     * 检查代码质量
     * AI 会分析代码并返回质量检查结果
     *
     * @param codeContent 代码内容（包含文件结构和具体代码）
     * @return 质量检查结果（结构化输出）
     */
    @SystemMessage(fromResource = "prompt/code-quality-check-system-prompt.txt")
    QualityResult checkCodeQuality(@UserMessage String codeContent);
}
