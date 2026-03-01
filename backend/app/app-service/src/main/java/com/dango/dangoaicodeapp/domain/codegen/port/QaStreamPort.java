package com.dango.dangoaicodeapp.domain.codegen.port;

import reactor.core.publisher.Flux;

/**
 * 问答流端口。
 */
public interface QaStreamPort {

    Flux<String> answer(long appId, String qaInput);
}

