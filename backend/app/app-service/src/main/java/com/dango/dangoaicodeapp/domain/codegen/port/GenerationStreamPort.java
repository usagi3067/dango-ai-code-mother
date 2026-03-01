package com.dango.dangoaicodeapp.domain.codegen.port;

import com.dango.dangoaicodeapp.domain.codegen.model.GenerationStreamChunk;

import java.util.List;

/**
 * 生成流端口。
 *
 * 统一抽象流式输出存储，避免应用层依赖 Redis Stream API。
 */
public interface GenerationStreamPort {

    /**
     * 追加流事件。
     * 统一由端口保证字段结构，避免调用方重复拼装存储格式。
     */
    void appendChunk(String streamKey, String content, String msgType);

    /**
     * 读取流事件（afterId 游标语义由端口适配）。
     * 上层只关心领域事件，不关心 Redis Record 结构。
     */
    List<GenerationStreamChunk> readChunks(String streamKey, String afterId, long count);
}
