package com.dango.dangoaicodeapp.domain.codegen.port;

import dev.langchain4j.service.TokenStream;

/**
 * 问答能力领域端口。
 */
public interface QaGateway {

    TokenStream answer(long appId, String qaInput);
}
