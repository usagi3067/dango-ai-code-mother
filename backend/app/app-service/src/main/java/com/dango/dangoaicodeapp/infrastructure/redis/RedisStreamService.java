package com.dango.dangoaicodeapp.infrastructure.redis;

import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.Resource;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class RedisStreamService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 向 Stream 追加消息（不设 MAXLEN，依赖 TTL 清理）
     */
    public RecordId addToStream(String streamKey, Map<String, String> fields) {
        StringRecord record = StreamRecords.string(fields).withStreamKey(streamKey);
        return stringRedisTemplate.opsForStream().add(record);
    }

    /**
     * 从指定 ID 之后读取消息（非阻塞）
     * @param afterId 起始 ID（不包含），传 "0" 从头读取
     * @param count 最大读取条数
     */
    public List<MapRecord<String, Object, Object>> readFromStream(
            String streamKey, String afterId, long count) {
        List<MapRecord<String, Object, Object>> records = stringRedisTemplate.opsForStream()
                .read(StreamReadOptions.empty().count(count),
                      StreamOffset.create(streamKey, ReadOffset.from(afterId)));
        return records != null ? records : Collections.emptyList();
    }

    /**
     * 阻塞读取（用于 SSE 推送等待新消息）
     * @param afterId 起始 ID
     * @param timeout 阻塞超时
     */
    public List<MapRecord<String, Object, Object>> blockingRead(
            String streamKey, String afterId, Duration timeout) {
        List<MapRecord<String, Object, Object>> records = stringRedisTemplate.opsForStream()
                .read(StreamReadOptions.empty().count(1).block(timeout),
                      StreamOffset.create(streamKey, ReadOffset.from(afterId)));
        return records != null ? records : Collections.emptyList();
    }

    /**
     * 设置 key 的过期时间
     */
    public void expire(String key, long seconds) {
        stringRedisTemplate.expire(key, seconds, TimeUnit.SECONDS);
    }

    /**
     * 删除 key
     */
    public void delete(String key) {
        stringRedisTemplate.delete(key);
    }

    /**
     * 检查 key 是否存在
     */
    public boolean exists(String key) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
    }
}
