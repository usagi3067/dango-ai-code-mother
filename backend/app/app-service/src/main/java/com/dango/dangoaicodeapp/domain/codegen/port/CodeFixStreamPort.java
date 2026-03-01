package com.dango.dangoaicodeapp.domain.codegen.port;

import com.dango.dangoaicodeapp.domain.app.valueobject.CodeGenTypeEnum;
import reactor.core.publisher.Flux;

/**
 * 代码修复流端口。
 *
 * <p>为何抽象：节点只表达“触发修复并消费流式消息”，
 * 不关心底层 TokenStream 回调与工具调用消息拼装细节。
 */
public interface CodeFixStreamPort {

    Flux<String> fixCodeStream(long appId, CodeGenTypeEnum codeGenType, String fixRequest);
}

