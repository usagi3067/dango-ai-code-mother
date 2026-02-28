package com.dango.dangoaicodeapp.domain.codegen.port;

import com.dango.dangoaicodeapp.domain.app.valueobject.CodeGenTypeEnum;
import dev.langchain4j.service.TokenStream;

/**
 * 代码生成领域端口。
 */
public interface CodeGenerationGateway {

    TokenStream generateCodeStream(long appId, CodeGenTypeEnum codeGenType, String userMessage);
}
