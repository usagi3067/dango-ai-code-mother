package com.dango.dangoaicodeapp.domain.codegen.port;

import reactor.core.publisher.Flux;

/**
 * 动画建议流端口。
 */
public interface AnimationAdvisorStreamPort {

    Flux<String> adviseLeetCode(String userPrompt);

    Flux<String> adviseInterview(String userPrompt);

    Flux<String> adviseInterviewSourceCode(String userPrompt);
}

