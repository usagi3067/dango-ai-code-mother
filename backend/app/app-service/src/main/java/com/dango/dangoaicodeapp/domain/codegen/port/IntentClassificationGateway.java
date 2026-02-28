package com.dango.dangoaicodeapp.domain.codegen.port;

/**
 * 意图识别领域端口。
 */
public interface IntentClassificationGateway {

    String classify(String classifyInput);
}
