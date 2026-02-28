package com.dango.dangoaicodeapp.domain.codegen.port;

import com.dango.dangoaicodeapp.domain.app.valueobject.CodeGenTypeEnum;
import dev.langchain4j.service.TokenStream;

/**
 * 代码修改领域端口。
 */
public interface CodeModificationGateway {

    TokenStream modifyCodeStream(long appId, CodeGenTypeEnum codeGenType, String modifyRequest);
}
