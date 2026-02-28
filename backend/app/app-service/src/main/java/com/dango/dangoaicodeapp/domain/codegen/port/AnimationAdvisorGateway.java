package com.dango.dangoaicodeapp.domain.codegen.port;

import dev.langchain4j.service.TokenStream;

/**
 * 动画建议能力领域端口。
 */
public interface AnimationAdvisorGateway {

    TokenStream adviseLeetCode(String userPrompt);

    TokenStream adviseInterview(String userPrompt);
}
