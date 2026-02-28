package com.dango.dangoaicodeapp.domain.codegen.port;

import com.dango.dangoaicodeapp.domain.app.valueobject.CodeGenTypeEnum;
import dev.langchain4j.service.TokenStream;

/**
 * 代码修复领域端口。
 */
public interface CodeFixGateway {

    TokenStream fixCodeStream(long appId, CodeGenTypeEnum codeGenType, String fixRequest);
}
