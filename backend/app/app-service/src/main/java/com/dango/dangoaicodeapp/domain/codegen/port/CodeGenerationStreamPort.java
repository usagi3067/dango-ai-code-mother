package com.dango.dangoaicodeapp.domain.codegen.port;

import com.dango.dangoaicodeapp.domain.app.valueobject.CodeGenTypeEnum;
import reactor.core.publisher.Flux;

/**
 * 代码生成流端口。
 *
 * <p>为何抽象：工作流节点只关心“拿到可持续消费的生成流”，
 * 不关心 AI SDK 回调、工具调用消息拼装等实现细节。
 */
public interface CodeGenerationStreamPort {

    Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId);
}

