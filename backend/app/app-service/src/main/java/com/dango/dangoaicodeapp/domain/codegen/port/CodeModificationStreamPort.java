package com.dango.dangoaicodeapp.domain.codegen.port;

import com.dango.dangoaicodeapp.domain.app.valueobject.CodeGenTypeEnum;
import reactor.core.publisher.Flux;

/**
 * 代码修改流端口。
 */
public interface CodeModificationStreamPort {

    Flux<String> modifyCodeStream(long appId, CodeGenTypeEnum codeGenType, String modifyRequest);
}

