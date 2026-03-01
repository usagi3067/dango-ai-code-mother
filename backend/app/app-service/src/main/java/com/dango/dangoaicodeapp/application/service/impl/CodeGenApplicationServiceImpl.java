package com.dango.dangoaicodeapp.application.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.dango.dangoaicodeapp.application.service.AppApplicationService;
import com.dango.dangoaicodeapp.application.service.ChatHistoryService;
import com.dango.dangoaicodeapp.application.service.CodeGenApplicationService;
import com.dango.dangoaicodeapp.domain.app.entity.App;
import com.dango.dangoaicodeapp.domain.app.repository.AppRepository;
import com.dango.dangoaicodeapp.domain.app.valueobject.CodeGenTypeEnum;
import com.dango.dangoaicodeapp.domain.app.valueobject.ElementInfo;
import com.dango.dangoaicodeapp.domain.codegen.handler.StreamHandlerExecutor;
import com.dango.dangoaicodeapp.domain.codegen.model.GenerationStreamChunk;
import com.dango.dangoaicodeapp.domain.codegen.model.GenerationSession;
import com.dango.dangoaicodeapp.domain.codegen.model.GenerationTaskSnapshot;
import com.dango.dangoaicodeapp.domain.codegen.service.GenerationSessionDomainService;
import com.dango.dangoaicodeapp.domain.codegen.workflow.command.RunWorkflowCommand;
import com.dango.dangoaicodeapp.infrastructure.config.AppProperties;
import com.dango.dangoaicodecommon.exception.BusinessException;
import com.dango.dangoaicodecommon.exception.ErrorCode;
import com.dango.dangoaicodecommon.exception.ThrowUtils;
import com.dango.dangoaicodecommon.monitor.MonitorContext;
import com.dango.dangoaicodecommon.monitor.MonitorContextHolder;
import com.dango.supabase.dto.TableSchemaDTO;
import com.dango.supabase.service.SupabaseService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.HashMap;
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
    private GenerationSessionDomainService generationSessionDomainService;

    @Resource
    private AppApplicationService appApplicationService;

    @Resource
    private AppProperties appProperties;

    @Resource
    private CodeGenWorkflowExecutor codeGenWorkflowExecutor;

    @DubboReference
    private SupabaseService supabaseService;

    @Override
    public boolean startBackgroundGeneration(Long appId, String message, ElementInfo elementInfo, long userId) {
        // 主流程保持“用例脚本化”：校验 -> 启动会话 -> 组装流 -> 订阅收口。
        // 细节下沉到私有方法，避免应用服务退化成大段基础设施实现代码。
        App app = loadAndCheckOwnership(appId, message, userId);
        saveUserMessageSafely(appId, userId, message);
        GenerationSession generationSession = generationSessionDomainService.startSession(appId, userId);
        try {
            Flux<String> processedStream = buildProcessedStream(app, message, appId, elementInfo, userId);
            subscribeGenerationStream(processedStream, generationSession, appId, userId);
            return true;
        } catch (Exception e) {
            log.error("启动后台生成流程失败: appId={}, userId={}", appId, userId, e);
            generationSessionDomainService.failSession(generationSession, e);
            MonitorContextHolder.clearContext();
            if (e instanceof BusinessException businessException) {
                throw businessException;
            }
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "后台生成任务启动失败");
        }
    }

    @Override
    public Flux<String> consumeGenerationStream(Long appId, long userId, String afterId) {
        return Flux.<String>create(sink -> {
            Schedulers.boundedElastic().schedule(() -> {
                String lastId = afterId != null ? afterId : "0";
                try {
                    while (!sink.isCancelled()) {
                        // 非阻塞读取已有消息
                        var records = generationSessionDomainService.readStreamChunks(appId, userId, lastId, 100);
                        lastId = emitStreamRecords(records, lastId, sink);
                        if (sink.isCancelled()) {
                            return;
                        }

                        GenerationTaskSnapshot taskSnapshot = generationSessionDomainService.getTaskSnapshot(appId, userId);
                        if (taskSnapshot.isTerminal() && records.isEmpty()) {
                            sink.complete();
                            return;
                        }
                        if (taskSnapshot.isNone()) {
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

    @Override
    public GenerationTaskSnapshot getGenerationStatus(Long appId, long userId) {
        return generationSessionDomainService.getTaskSnapshot(appId, userId);
    }

    private App loadAndCheckOwnership(Long appId, String message, long userId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "用户消息不能为空");
        App app = appRepository.findById(appId).orElse(null);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        if (!app.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限访问该应用");
        }
        return app;
    }

    private void saveUserMessageSafely(Long appId, long userId, String message) {
        try {
            chatHistoryService.saveUserMessage(appId, userId, message);
        } catch (Exception e) {
            log.error("保存用户消息失败: {}", e.getMessage(), e);
        }
    }

    private Flux<String> buildProcessedStream(
            App app, String message, Long appId, ElementInfo elementInfo, long userId) {
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(app.getCodeGenType());
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的代码生成类型");
        }

        boolean databaseEnabled = Boolean.TRUE.equals(app.getHasDatabase());
        String databaseSchema = loadDatabaseSchema(appId, databaseEnabled);
        MonitorContext monitorContext = buildMonitorContext(appId, userId);

        RunWorkflowCommand command = RunWorkflowCommand.builder()
                .originalPrompt(message)
                .appId(appId)
                .elementInfo(elementInfo)
                .databaseEnabled(databaseEnabled)
                .databaseSchema(databaseSchema)
                .generationType(codeGenTypeEnum)
                .build();

        Flux<String> codeStream = codeGenWorkflowExecutor.executeWithFlux(command, monitorContext);
        return streamHandlerExecutor.doExecute(codeStream);
    }

    private void subscribeGenerationStream(
            Flux<String> processedStream, GenerationSession session, Long appId, long userId) {
        // 统一订阅收口：onNext/onComplete/onError 都走领域服务，
        // 保证“任务状态 + 聊天消息状态”的一致性规则不会散落在多个分支里。
        StringBuilder fullContentBuilder = new StringBuilder();
        processedStream
                .subscribeOn(Schedulers.boundedElastic())
                .timeout(Duration.ofMinutes(5))
                .doOnNext(chunk -> appendChunk(session, chunk, fullContentBuilder))
                .doOnComplete(() -> completeSession(session, fullContentBuilder.toString(), appId, userId))
                .doOnError(error -> failSession(session, error, fullContentBuilder.toString(), appId, userId))
                .subscribe();
    }

    private String emitStreamRecords(
            List<GenerationStreamChunk> records, String lastId, FluxSink<String> sink) {
        // 抽成独立方法：把“遍历、取消检查、游标推进”封装成一个原子步骤，
        // 让 consume 主循环只表达控制流（读流 -> 判状态 -> sleep）。
        String nextLastId = lastId;
        for (GenerationStreamChunk record : records) {
            if (sink.isCancelled()) {
                return nextLastId;
            }
            sink.next(toStreamPayload(record));
            if (record.id() != null) {
                nextLastId = record.id();
            }
        }
        return nextLastId;
    }

    private String toStreamPayload(GenerationStreamChunk record) {
        // 序列化集中在一处，避免不同调用点出现字段不一致（d/msgType）的问题。
        HashMap<String, String> msg = new HashMap<>();
        msg.put("d", record.content());
        if (record.msgType() != null) {
            msg.put("msgType", record.msgType());
        }
        return JSONUtil.toJsonStr(msg);
    }

    private void appendChunk(GenerationSession session, String chunk, StringBuilder fullContentBuilder) {
        cn.hutool.json.JSONObject jsonObj = JSONUtil.parseObj(chunk);
        String content = jsonObj.getStr("d", "");
        String msgType = jsonObj.getStr("msgType");
        if (!"log".equals(msgType)) {
            fullContentBuilder.append(content);
        }
        generationSessionDomainService.appendChunk(session, content, msgType);
    }

    private void completeSession(GenerationSession session, String fullContent, Long appId, long userId) {
        generationSessionDomainService.completeSession(session, fullContent);
        MonitorContextHolder.clearContext();
        log.info("后台生成任务完成: appId={}, userId={}", appId, userId);
        triggerAppScreenshotSafely(appId);
    }

    private void failSession(
            GenerationSession session, Throwable throwable, String partialContent, Long appId, long userId) {
        log.error("后台生成任务失败: appId={}, userId={}, error={}", appId, userId, throwable.getMessage());
        if (StrUtil.isNotBlank(partialContent)) {
            generationSessionDomainService.failSession(session, partialContent);
        } else {
            generationSessionDomainService.failSession(session, throwable);
        }
        MonitorContextHolder.clearContext();
    }

    private void triggerAppScreenshotSafely(Long appId) {
        try {
            App completedApp = appRepository.findById(appId).orElse(null);
            if (completedApp == null || completedApp.getCodeGenType() == null) {
                return;
            }
            String previewUrl = String.format("%s/api/static/%s_%s/dist/index.html",
                    appProperties.getPreviewHost(), completedApp.getCodeGenType(), appId);
            appApplicationService.generateAppScreenshotAsync(appId, previewUrl);
            log.info("已触发生成完成截图: appId={}", appId);
        } catch (Exception e) {
            log.warn("触发截图失败（不影响主流程）: appId={}, error={}", appId, e.getMessage());
        }
    }

    private String loadDatabaseSchema(Long appId, boolean databaseEnabled) {
        if (!databaseEnabled) {
            return null;
        }
        try {
            var tables = supabaseService.getSchema(appId);
            if (tables == null || tables.isEmpty()) {
                return null;
            }
            log.info("应用 {} 已启用数据库，当前有 {} 个表", appId, tables.size());
            return formatTableSchemas(tables);
        } catch (Exception e) {
            log.error("查询数据库 Schema 失败: {}", e.getMessage(), e);
            return null;
        }
    }

    private MonitorContext buildMonitorContext(Long appId, long userId) {
        return MonitorContext.builder()
                .userId(String.valueOf(userId))
                .appId(appId.toString())
                .build();
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
