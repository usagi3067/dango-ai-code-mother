package com.dango.dangoaicodeapp.domain.codegen.port;

/**
 * 意图识别领域端口。
 */
public interface IntentClassificationPort {

    String classify(String classifyInput);
}
