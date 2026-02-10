package com.dango.aicodegenerate.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

/**
 * AI 代码修复服务接口
 * 用于修复模式下的代码修复操作
 * 根据质检错误信息进行针对性修复
 */
public interface AiCodeFixerService {

    /**
     * 修复 HTML 单文件代码（流式）
     *
     * @param appId      应用 ID（用于记忆管理）
     * @param fixRequest 修复请求（包含错误信息、修复建议、原始需求）
     * @return 修复过程的流式响应
     */
    @SystemMessage(fromResource = "prompt/codegen-fix-system-prompt.txt")
    TokenStream fixHtmlCodeStream(@MemoryId long appId, @UserMessage String fixRequest);

    /**
     * 修复多文件项目代码（流式）
     *
     * @param appId      应用 ID（用于记忆管理）
     * @param fixRequest 修复请求（包含错误信息、修复建议、原始需求）
     * @return 修复过程的流式响应
     */
    @SystemMessage(fromResource = "prompt/codegen-fix-multi-file-system-prompt.txt")
    TokenStream fixMultiFileCodeStream(@MemoryId long appId, @UserMessage String fixRequest);

    /**
     * 修复 Vue 项目代码（流式）
     *
     * @param appId      应用 ID（用于记忆管理）
     * @param fixRequest 修复请求（包含错误信息、修复建议、原始需求）
     * @return 修复过程的流式响应
     */
    @SystemMessage(fromResource = "prompt/codegen-fix-vue-project-system-prompt.txt")
    TokenStream fixVueProjectCodeStream(@MemoryId long appId, @UserMessage String fixRequest);
}
