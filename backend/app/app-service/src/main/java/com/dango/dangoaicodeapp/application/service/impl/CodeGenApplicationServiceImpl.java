package com.dango.dangoaicodeapp.application.service.impl;

import cn.hutool.core.util.StrUtil;

import com.dango.dangoaicodeapp.application.service.ChatHistoryService;
import com.dango.dangoaicodeapp.application.service.CodeGenApplicationService;
import com.dango.dangoaicodeapp.domain.app.entity.App;
import com.dango.dangoaicodeapp.domain.app.repository.AppRepository;
import com.dango.dangoaicodeapp.domain.app.valueobject.CodeGenTypeEnum;
import com.dango.dangoaicodeapp.domain.app.valueobject.ElementInfo;
import com.dango.dangoaicodeapp.domain.codegen.handler.StreamHandlerExecutor;
import com.dango.dangoaicodeapp.domain.codegen.workflow.CodeGenWorkflow;
import com.dango.dangoaicodeapp.infrastructure.monitor.MonitorContext;
import com.dango.dangoaicodeapp.infrastructure.monitor.MonitorContextHolder;
import com.dango.dangoaicodeapp.infrastructure.redis.GenTaskService;
import com.dango.dangoaicodeapp.infrastructure.redis.RedisStreamService;
import com.dango.dangoaicodecommon.exception.BusinessException;
import com.dango.dangoaicodecommon.exception.ErrorCode;
import com.dango.dangoaicodecommon.exception.ThrowUtils;
import com.dango.supabase.dto.TableSchemaDTO;
import com.dango.supabase.service.SupabaseService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 代码生成 应用服务层实现
 *
 * @author dango
 */
@Slf4j
@Service
public class CodeGenApplicationServiceImpl implements CodeGenApplicationService {

    @Resource
    private AppRepository appRepository;
    @Resource
    private ChatHistoryService chatHistoryService;
    @Resource
    private StreamHandlerExecutor streamHandlerExecutor;
    @Resource
    private RedisStreamService redisStreamService;
    @Resource
    private GenTaskService genTaskService;
    @DubboReference
    private SupabaseService supabaseService;

    @Override
    public Flux<String> chatToGenCode(Long appId, String message, ElementInfo elementInfo, long userId) {
        // 1. 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "用户消息不能为空");
        // 2. 查询应用信息
        App app = appRepository.findById(appId).orElse(null);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3. 验证用户是否有权限访问该应用，仅本人可以生成代码
        if (!app.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限访问该应用");
        }
        // 4. 获取应用的代码生成类型
        String codeGenTypeStr = app.getCodeGenType();
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenTypeStr);
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的代码生成类型");
        }
        // 5. 保存用户消息到对话历史
        try {
            chatHistoryService.saveUserMessage(appId, userId, message);
        } catch (Exception e) {
            log.error("保存用户消息失败: {}", e.getMessage());
        }
        // 6. 检查数据库是否启用，如果启用则查询当前 Schema
        boolean databaseEnabled = Boolean.TRUE.equals(app.getHasDatabase());
        String databaseSchema = null;
        if (databaseEnabled) {
            try {
                List<TableSchemaDTO> tables = supabaseService.getSchema(appId);
                if (tables != null && !tables.isEmpty()) {
                    databaseSchema = formatTableSchemas(tables);
                    log.info("应用 {} 已启用数据库，当前有 {} 个表", appId, tables.size());
                }
            } catch (Exception e) {
                log.error("查询数据库 Schema 失败: {}", e.getMessage(), e);
            }
        }
        // 7. 创建监控上下文
        MonitorContext monitorContext = MonitorContext.builder()
                .userId(String.valueOf(userId))
                .appId(appId.toString())
                .build();
        MonitorContextHolder.setContext(monitorContext);
        // 8. 使用 Agent 模式（工作流）生成代码
        log.info("使用 Agent 模式（工作流）生成代码, appId: {}, hasElementInfo: {}, databaseEnabled: {}",
                appId, elementInfo != null, databaseEnabled);
        Flux<String> codeStream = new CodeGenWorkflow().executeWorkflowWithFlux(
                message, appId, elementInfo, databaseEnabled, databaseSchema, monitorContext, codeGenTypeEnum);
        // 9. 收集 AI 响应内容并在完成后记录到对话历史
        return streamHandlerExecutor.doExecute(codeStream, chatHistoryService, appId, userId)
                .doFinally(signalType -> {
                    MonitorContextHolder.clearContext();
                });
    }

    @Override
    public boolean startBackgroundGeneration(Long appId, String message, ElementInfo elementInfo, long userId) {
        // 1. 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "用户消息不能为空");
        App app = appRepository.findById(appId).orElse(null);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        if (!app.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限访问该应用");
        }

        // 2. 保存用户消息
        try {
            chatHistoryService.saveUserMessage(appId, userId, message);
        } catch (Exception e) {
            log.error("保存用户消息失败: {}", e.getMessage());
        }

        // 3. 预插入 generating 状态的 AI 消息
        Long chatHistoryId = chatHistoryService.saveAiMessageWithStatus(appId, userId, "", "generating");

        // 4. CAS 防重复启动
        if (!genTaskService.tryStartTask(appId, userId, chatHistoryId)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "已有生成任务在运行中");
        }

        String streamKey = genTaskService.getStreamKey(appId, userId);

        // 5. 构建 AI 生成 Flux
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(app.getCodeGenType());
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的代码生成类型");
        }

        boolean databaseEnabled = Boolean.TRUE.equals(app.getHasDatabase());
        String databaseSchema = null;
        if (databaseEnabled) {
            try {
                var tables = supabaseService.getSchema(appId);
                if (tables != null && !tables.isEmpty()) {
                    databaseSchema = formatTableSchemas(tables);
                    log.info("应用 {} 已启用数据库，当前有 {} 个表", appId, tables.size());
                }
            } catch (Exception e) {
                log.error("查询数据库 Schema 失败: {}", e.getMessage(), e);
            }
        }

        MonitorContext monitorContext = MonitorContext.builder()
                .userId(String.valueOf(userId))
                .appId(appId.toString())
                .build();

        Flux<String> codeStream = new CodeGenWorkflow().executeWorkflowWithFlux(
                message, appId, elementInfo, databaseEnabled, databaseSchema, monitorContext, codeGenTypeEnum);

        // 6. 经过 StreamHandler 处理（解析 JSON 消息类型，但不保存历史）
        Flux<String> processedStream = streamHandlerExecutor.doExecuteWithoutSave(codeStream);

        // 7. 独立订阅，写入 Redis Stream
        StringBuilder fullContentBuilder = new StringBuilder();

        processedStream
            .subscribeOn(Schedulers.boundedElastic())
            .timeout(Duration.ofMinutes(5))
            .doOnNext(chunk -> {
                fullContentBuilder.append(chunk);
                redisStreamService.addToStream(streamKey, java.util.Map.of("d", chunk));
            })
            .doOnComplete(() -> {
                genTaskService.markCompleted(appId, userId);
                chatHistoryService.updateAiMessage(chatHistoryId, fullContentBuilder.toString(), "completed");
                MonitorContextHolder.clearContext();
                log.info("后台生成任务完成: appId={}, userId={}", appId, userId);
            })
            .doOnError(error -> {
                log.error("后台生成任务失败: appId={}, userId={}, error={}", appId, userId, error.getMessage());
                genTaskService.markError(appId, userId);
                String partialContent = fullContentBuilder.toString();
                chatHistoryService.updateAiMessage(chatHistoryId,
                        StrUtil.isNotBlank(partialContent) ? partialContent : "AI回复失败: " + error.getMessage(),
                        "error");
                MonitorContextHolder.clearContext();
            })
            .subscribe();

        return true;
    }

    @Override
    public Flux<String> consumeGenerationStream(Long appId, long userId, String afterId) {
        String streamKey = genTaskService.getStreamKey(appId, userId);

        return Flux.<String>create(sink -> {
            Schedulers.boundedElastic().schedule(() -> {
                String lastId = afterId != null ? afterId : "0";
                try {
                    while (!sink.isCancelled()) {
                        // 非阻塞读取已有消息
                        var records = redisStreamService.readFromStream(streamKey, lastId, 100);
                        for (var record : records) {
                            if (sink.isCancelled()) return;
                            Object d = record.getValue().get("d");
                            if (d != null) {
                                sink.next(d.toString());
                            }
                            lastId = record.getId().getValue();
                        }

                        // 检查任务状态
                        String status = genTaskService.getStatus(appId, userId);

                        // 如果任务已完成且没有更多消息，结束
                        if (("completed".equals(status) || "error".equals(status)) && records.isEmpty()) {
                            sink.complete();
                            return;
                        }

                        // 如果状态是 none（任务不存在），也结束
                        if ("none".equals(status)) {
                            sink.complete();
                            return;
                        }

                        // 没有新消息时短暂休眠，避免空转（不使用 XREAD BLOCK，防止阻塞共享连接）
                        if (records.isEmpty()) {
                            Thread.sleep(200);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    if (!sink.isCancelled()) {
                        sink.complete();
                    }
                } catch (Exception e) {
                    if (!sink.isCancelled()) {
                        log.error("消费 Stream 异常: {}", e.getMessage());
                        sink.error(e);
                    }
                }
            });
        });
    }

    /**
     * 格式化表结构为字符串
     * 用于传递给工作流上下文
     *
     * @param tables 表结构列表（扁平结构，每行代表一个列）
     * @return 格式化后的字符串
     */
    private String formatTableSchemas(List<TableSchemaDTO> tables) {
        if (tables == null || tables.isEmpty()) {
            return "";
        }

        Map<String, List<TableSchemaDTO>> tableMap = tables.stream()
                .collect(Collectors.groupingBy(TableSchemaDTO::getTableName));

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, List<TableSchemaDTO>> entry : tableMap.entrySet()) {
            String tableName = entry.getKey();
            List<TableSchemaDTO> columns = entry.getValue();

            sb.append("表 ").append(tableName).append(":\n");
            for (TableSchemaDTO column : columns) {
                sb.append("  - ").append(column.getColumnName())
                        .append(": ").append(column.getDataType());
                if (Boolean.TRUE.equals(column.getIsNullable())) {
                    sb.append(" (nullable)");
                }
                if (StrUtil.isNotBlank(column.getColumnDefault())) {
                    sb.append(" DEFAULT ").append(column.getColumnDefault());
                }
                sb.append("\n");
            }
        }
        return sb.toString();
    }
}
