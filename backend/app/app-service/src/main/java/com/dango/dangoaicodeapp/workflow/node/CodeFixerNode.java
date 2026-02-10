package com.dango.dangoaicodeapp.workflow.node;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.dango.aicodegenerate.model.QualityResult;
import com.dango.aicodegenerate.model.message.AiResponseMessage;
import com.dango.aicodegenerate.model.message.ToolExecutedMessage;
import com.dango.aicodegenerate.model.message.ToolRequestMessage;
import com.dango.aicodegenerate.service.AiCodeFixerService;
import com.dango.dangoaicodeapp.ai.AiCodeFixerServiceFactory;
import com.dango.dangoaicodeapp.model.enums.CodeGenTypeEnum;
import com.dango.dangoaicodeapp.workflow.state.WorkflowContext;
import com.dango.dangoaicodecommon.utils.SpringContextUtil;
import dev.langchain4j.service.TokenStream;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 代码修复节点
 * 根据质检错误信息进行针对性修复
 * 
 * 功能说明：
 * - 从 WorkflowContext 获取质检结果中的错误列表和修复建议
 * - 构建修复请求，调用 AI 服务进行代码修复
 * - 支持多次循环修复直到质检通过或达到最大重试次数
 *
 * @author dango
 */
@Slf4j
@Component
public class CodeFixerNode {

    private static final String NODE_NAME = "代码修复";

    /**
     * 创建节点动作
     */
    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: {}", NODE_NAME);

            // 发送节点开始消息
            context.emitNodeStart(NODE_NAME);

            // 增加重试计数
            int retryCount = context.getFixRetryCount() + 1;
            context.setFixRetryCount(retryCount);

            log.info("代码修复尝试次数: {}/{}", retryCount, WorkflowContext.MAX_FIX_RETRY_COUNT);
            context.emitNodeMessage(NODE_NAME, 
                    String.format("修复尝试 %d/%d\n", retryCount, WorkflowContext.MAX_FIX_RETRY_COUNT));

            // 检查是否达到最大重试次数
            if (retryCount > WorkflowContext.MAX_FIX_RETRY_COUNT) {
                log.warn("达到最大修复重试次数 ({})，使用当前代码作为最终结果", 
                        WorkflowContext.MAX_FIX_RETRY_COUNT);
                context.emitNodeMessage(NODE_NAME, "⚠️ 达到最大修复次数，使用当前代码作为最终结果\n");
                
                // 强制通过质检，结束修复循环
                QualityResult qualityResult = context.getQualityResult();
                if (qualityResult != null) {
                    qualityResult.setIsValid(true);
                }
                
                context.emitNodeComplete(NODE_NAME);
                context.setCurrentStep(NODE_NAME);
                return WorkflowContext.saveContext(context);
            }

            try {
                // 恢复监控上下文到当前线程（用于跨线程传递监控信息）
                context.restoreMonitorContext();
                
                // 构建修复请求
                String fixRequest = buildFixRequest(context);
                
                log.info("修复请求构建完成:\n{}", fixRequest);
                context.emitNodeMessage(NODE_NAME, "正在分析错误并修复代码...\n");

                Long appId = context.getAppId();
                CodeGenTypeEnum generationType = context.getGenerationType();
                
                // 如果没有设置代码生成类型，默认使用 HTML
                if (generationType == null) {
                    generationType = CodeGenTypeEnum.HTML;
                    context.setGenerationType(generationType);
                }

                // 获取修复专用 AI 服务
                AiCodeFixerServiceFactory fixerServiceFactory = SpringContextUtil.getBean(AiCodeFixerServiceFactory.class);
                AiCodeFixerService fixerService = fixerServiceFactory.getFixerService(appId, generationType);
                
                // 根据代码生成类型选择对应的修复方法
                TokenStream tokenStream = switch (generationType) {
                    case HTML -> fixerService.fixHtmlCodeStream(appId, fixRequest);
                    case MULTI_FILE -> fixerService.fixMultiFileCodeStream(appId, fixRequest);
                    case VUE_PROJECT -> fixerService.fixVueProjectCodeStream(appId, fixRequest);
                };
                
                // 使用 CountDownLatch 等待流式生成完成
                CountDownLatch latch = new CountDownLatch(1);
                AtomicReference<Throwable> errorRef = new AtomicReference<>();
                
                // 订阅 TokenStream，实时输出到前端
                // 注意：必须注册所有回调，包括工具调用相关的回调，否则会导致 NPE
                tokenStream
                        .onPartialResponse(partialResponse -> {
                            // 实时输出代码片段到前端（包装为 JSON 格式）
                            AiResponseMessage aiResponseMessage = new AiResponseMessage(partialResponse);
                            context.emit(JSONUtil.toJsonStr(aiResponseMessage));
                        })
                        .onPartialToolExecutionRequest((index, toolExecutionRequest) -> {
                            // 处理工具调用请求（流式输出工具调用信息）
                            ToolRequestMessage toolRequestMessage = new ToolRequestMessage(toolExecutionRequest);
                            context.emit(JSONUtil.toJsonStr(toolRequestMessage));
                        })
                        .onToolExecuted(toolExecution -> {
                            // 处理工具执行结果
                            ToolExecutedMessage toolExecutedMessage = new ToolExecutedMessage(toolExecution);
                            context.emit(JSONUtil.toJsonStr(toolExecutedMessage));
                        })
                        .onCompleteResponse(response -> {
                            log.info("代码修复完成");
                            latch.countDown();
                        })
                        .onError(error -> {
                            log.error("代码修复失败: {}", error.getMessage(), error);
                            errorRef.set(error);
                            latch.countDown();
                        })
                        .start();
                
                // 等待流式生成完成
                latch.await();
                
                // 检查是否有错误
                if (errorRef.get() != null) {
                    throw new RuntimeException(errorRef.get());
                }

                context.emitNodeMessage(NODE_NAME, "\n代码修复完成，准备重新质检\n");

                // 清除质量检查结果，准备重新检查
                context.setQualityResult(null);

            } catch (Exception e) {
                log.error("代码修复失败: {}", e.getMessage(), e);
                context.setErrorMessage("代码修复失败: " + e.getMessage());
                context.emitNodeError(NODE_NAME, e.getMessage());
            } finally {
                // 清除当前线程的监控上下文
                context.clearMonitorContext();
            }

            // 发送节点完成消息
            context.emitNodeComplete(NODE_NAME);

            // 更新状态
            context.setCurrentStep(NODE_NAME);
            return WorkflowContext.saveContext(context);
        });
    }

    /**
     * 构建修复请求
     * 包含：原始需求 + 错误信息 + 修复建议
     * 根据不同的代码生成类型，添加不同的输出格式说明
     *
     * @param context 工作流上下文
     * @return 修复请求字符串
     */
    public static String buildFixRequest(WorkflowContext context) {
        StringBuilder request = new StringBuilder();

        // 添加原始需求
        String originalPrompt = context.getOriginalPrompt();
        if (StrUtil.isNotBlank(originalPrompt)) {
            request.append("## 原始需求\n")
                   .append(originalPrompt)
                   .append("\n\n");
        }

        // 获取质量检查结果
        QualityResult qualityResult = context.getQualityResult();
        
        // 添加错误信息
        request.append("## 代码存在以下问题，请修复\n");
        if (qualityResult != null && CollUtil.isNotEmpty(qualityResult.getErrors())) {
            List<String> errors = qualityResult.getErrors();
            for (int i = 0; i < errors.size(); i++) {
                request.append(String.format("%d. %s\n", i + 1, errors.get(i)));
            }
        } else {
            request.append("- 代码质量检查未通过，请检查并修复潜在问题\n");
        }
        request.append("\n");

        // 添加修复建议
        if (qualityResult != null && CollUtil.isNotEmpty(qualityResult.getSuggestions())) {
            request.append("## 修复建议\n");
            List<String> suggestions = qualityResult.getSuggestions();
            for (int i = 0; i < suggestions.size(); i++) {
                request.append(String.format("%d. %s\n", i + 1, suggestions.get(i)));
            }
            request.append("\n");
        }

        // 根据代码生成类型添加不同的输出格式说明
        CodeGenTypeEnum generationType = context.getGenerationType();
        request.append(getOutputFormatGuide(generationType));

        return request.toString();
    }

    /**
     * 根据代码生成类型获取输出格式指南
     *
     * @param generationType 代码生成类型
     * @return 输出格式指南字符串
     */
    public static String getOutputFormatGuide(CodeGenTypeEnum generationType) {
        if (generationType == null) {
            generationType = CodeGenTypeEnum.HTML;
        }

        return switch (generationType) {
            case HTML -> """
                    ## 修复指南（HTML 单文件模式）
                    1. 仔细阅读上述错误列表，理解每个问题的具体原因
                    2. 参考修复建议，确定修复方案
                    3. 按照问题的严重程度依次修复
                    4. 确保修复不会引入新的问题
                    5. 输出完整的修复后代码（单个 HTML 文件，包含内联 CSS 和 JS）
                    
                    **重要**: 最多输出 1 个 HTML 代码块，否则会导致保存错误！
                    """;
            case MULTI_FILE -> """
                    ## 修复指南（多文件模式）
                    1. 仔细阅读上述错误列表，理解每个问题的具体原因
                    2. 确定问题所在的文件（HTML/CSS/JS）
                    3. 参考修复建议，确定修复方案
                    4. 按照问题的严重程度依次修复
                    5. 确保修复不会引入新的问题
                    6. 输出完整的三个文件代码
                    
                    **重要**: 必须输出 3 个代码块（HTML + CSS + JavaScript），每种语言只能有 1 个代码块！
                    """;
            case VUE_PROJECT -> """
                    ## 修复指南（Vue 工程模式）
                    1. 仔细阅读上述错误列表，理解每个问题的具体原因
                    2. 使用【目录读取工具】了解当前项目结构
                    3. 使用【文件读取工具】查看需要修复的文件内容
                    4. 根据错误信息，使用对应的工具进行修复：
                       - 【文件修改工具】：修改现有文件的部分内容
                       - 【文件写入工具】：创建新文件或完全重写文件
                    5. 确保修复不会引入新的问题
                    
                    **重要**: 必须使用工具进行修复，不要直接输出代码块！
                    """;
        };
    }

    /**
     * 检查是否应该继续修复
     * 用于条件边判断
     *
     * @param context 工作流上下文
     * @return true 如果应该继续修复，false 如果应该结束
     */
    public static boolean shouldContinueFix(WorkflowContext context) {
        // 检查质检结果
        QualityResult qualityResult = context.getQualityResult();
        if (qualityResult == null || !qualityResult.getIsValid()) {
            // 质检未通过，检查重试次数
            return context.getFixRetryCount() < WorkflowContext.MAX_FIX_RETRY_COUNT;
        }
        // 质检通过，不需要继续修复
        return false;
    }

    /**
     * 检查是否达到最大重试次数
     *
     * @param context 工作流上下文
     * @return true 如果达到最大重试次数
     */
    public static boolean hasReachedMaxRetry(WorkflowContext context) {
        return context.getFixRetryCount() >= WorkflowContext.MAX_FIX_RETRY_COUNT;
    }
}
