package com.dango.dangoaicodeapp.infrastructure.redis;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class GenTaskService {

    private static final String TASK_KEY_PREFIX = "gen:task:";
    private static final String STREAM_KEY_PREFIX = "gen:stream:";
    private static final long COMPLETED_TTL_SECONDS = 600; // 10 分钟

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public String getTaskKey(Long appId, Long userId) {
        return TASK_KEY_PREFIX + appId + ":" + userId;
    }

    public String getStreamKey(Long appId, Long userId) {
        return STREAM_KEY_PREFIX + appId + ":" + userId;
    }

    /**
     * 尝试启动生成任务（CAS 防重复）
     * @return true 表示成功启动，false 表示已有任务在运行
     */
    public boolean tryStartTask(Long appId, Long userId, Long chatHistoryId) {
        String taskKey = getTaskKey(appId, userId);
        // HSETNX：仅当 field 不存在时设置，用于防重复
        Boolean set = stringRedisTemplate.opsForHash().putIfAbsent(taskKey, "status", "generating");
        if (Boolean.FALSE.equals(set)) {
            // 已有任务，先检查状态
            String existingStatus = (String) stringRedisTemplate.opsForHash().get(taskKey, "status");
            if ("completed".equals(existingStatus) || "error".equals(existingStatus)) {
                // 已完成或已失败的任务，清理后允许新任务启动
                log.info("清理已结束的任务(status={}): appId={}, userId={}", existingStatus, appId, userId);
                cleanupTask(appId, userId);
            } else {
                // 状态为 generating，检查是否是僵死任务（超过 10 分钟）
                String startTime = (String) stringRedisTemplate.opsForHash().get(taskKey, "startTime");
                if (startTime != null) {
                    long elapsed = System.currentTimeMillis() - Long.parseLong(startTime);
                    if (elapsed > 10 * 60 * 1000) {
                        log.warn("检测到僵死任务，强制清理: appId={}, userId={}", appId, userId);
                        cleanupTask(appId, userId);
                    } else {
                        return false;
                    }
                }
            }
        }
        stringRedisTemplate.opsForHash().putAll(taskKey, Map.of(
                "status", "generating",
                "startTime", String.valueOf(System.currentTimeMillis()),
                "chatHistoryId", chatHistoryId.toString()
        ));
        return true;
    }

    /**
     * 获取任务状态
     */
    public String getStatus(Long appId, Long userId) {
        String taskKey = getTaskKey(appId, userId);
        Object status = stringRedisTemplate.opsForHash().get(taskKey, "status");
        return status != null ? status.toString() : "none";
    }

    /**
     * 获取任务关联的 chatHistoryId
     */
    public Long getChatHistoryId(Long appId, Long userId) {
        String taskKey = getTaskKey(appId, userId);
        Object id = stringRedisTemplate.opsForHash().get(taskKey, "chatHistoryId");
        return id != null ? Long.parseLong(id.toString()) : null;
    }

    /**
     * 标记任务完成，设置 TTL
     */
    public void markCompleted(Long appId, Long userId) {
        String taskKey = getTaskKey(appId, userId);
        String streamKey = getStreamKey(appId, userId);
        stringRedisTemplate.opsForHash().put(taskKey, "status", "completed");
        stringRedisTemplate.expire(taskKey, COMPLETED_TTL_SECONDS, TimeUnit.SECONDS);
        stringRedisTemplate.expire(streamKey, COMPLETED_TTL_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * 标记任务失败，设置 TTL
     */
    public void markError(Long appId, Long userId) {
        String taskKey = getTaskKey(appId, userId);
        String streamKey = getStreamKey(appId, userId);
        stringRedisTemplate.opsForHash().put(taskKey, "status", "error");
        stringRedisTemplate.expire(taskKey, COMPLETED_TTL_SECONDS, TimeUnit.SECONDS);
        stringRedisTemplate.expire(streamKey, COMPLETED_TTL_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * 清理任务（删除 task 和 stream）
     */
    public void cleanupTask(Long appId, Long userId) {
        stringRedisTemplate.delete(getTaskKey(appId, userId));
        stringRedisTemplate.delete(getStreamKey(appId, userId));
    }
}
