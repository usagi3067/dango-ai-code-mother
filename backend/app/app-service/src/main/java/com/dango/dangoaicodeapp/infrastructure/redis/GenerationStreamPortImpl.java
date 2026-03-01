package com.dango.dangoaicodeapp.infrastructure.redis;

import com.dango.dangoaicodeapp.domain.codegen.model.GenerationStreamChunk;
import com.dango.dangoaicodeapp.domain.codegen.port.GenerationStreamPort;
import jakarta.annotation.Resource;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Redis Stream 适配器。
 */
@Component
public class GenerationStreamPortImpl implements GenerationStreamPort {

    @Resource
    private RedisStreamService redisStreamService;

    @Override
    public void appendChunk(String streamKey, String content, String msgType) {
        // 固化流字段协议（d/msgType），让上层不感知 Redis Stream schema。
        Map<String, String> streamData = new HashMap<>();
        streamData.put("d", content == null ? "" : content);
        if (msgType != null) {
            streamData.put("msgType", msgType);
        }
        redisStreamService.addToStream(streamKey, streamData);
    }

    @Override
    public List<GenerationStreamChunk> readChunks(String streamKey, String afterId, long count) {
        // 在适配层完成 Redis Record -> 领域事件映射，调用方只消费领域模型。
        return redisStreamService.readFromStream(streamKey, afterId, count)
                .stream()
                .map(this::toChunk)
                .toList();
    }

    private GenerationStreamChunk toChunk(MapRecord<String, Object, Object> record) {
        Object content = record.getValue().get("d");
        Object msgType = record.getValue().get("msgType");
        return new GenerationStreamChunk(
                record.getId() != null ? record.getId().getValue() : null,
                content != null ? content.toString() : "",
                msgType != null ? msgType.toString() : null
        );
    }
}
