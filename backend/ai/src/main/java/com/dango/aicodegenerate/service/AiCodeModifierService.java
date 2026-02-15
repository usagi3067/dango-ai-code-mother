package com.dango.aicodegenerate.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

/**
 * AI 代码修改服务接口
 * 用于修改模式下的代码修改操作
 * 配置了文件操作工具和图片工具，AI 可自行调用
 */
public interface AiCodeModifierService {

    /**
     * 修改 Vue 项目代码（流式）
     *
     * @param appId 应用 ID（用于记忆管理）
     * @param modifyRequest 修改请求（包含项目结构、元素信息、修改要求）
     * @return 修改过程的流式响应
     */
    @SystemMessage(fromResource = "prompt/codegen-modify-vue-project-system-prompt.txt")
    TokenStream modifyVueProjectCodeStream(@MemoryId long appId, @UserMessage String modifyRequest);
}
