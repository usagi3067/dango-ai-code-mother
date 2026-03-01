package com.dango.dangoaicodeapp.domain.codegen.model;

/**
 * 代码生成流事件。
 */
public record GenerationStreamChunk(String id, String content, String msgType) {

    public GenerationStreamChunk {
        content = content == null ? "" : content;
    }
}
