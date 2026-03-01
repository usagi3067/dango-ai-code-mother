package com.dango.dangoaicodeapp.domain.codegen.node;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.dango.aicodegenerate.model.QualityResult;
import com.dango.dangoaicodeapp.domain.app.valueobject.CodeGenTypeEnum;
import com.dango.dangoaicodeapp.domain.codegen.port.CodeFixStreamPort;
import com.dango.dangoaicodeapp.domain.codegen.workflow.state.WorkflowContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 代码修复节点
 * 根据真实编译器（npm run build）输出的构建错误进行针对性修复
 *
 * 功能说明：
 * - 从 WorkflowContext 获取 BuildCheckNode 提取的构建错误信息
 * - 构建修复请求，调用 AI 服务进行代码修复
 * - 支持多次循环修复直到构建通过或达到最大重试次数
 * - 禁止修改模板基础设施文件（package.json、vite.config.js、src/main.js、index.html）
 *
 * @author dango
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CodeFixerNode {

    private static final String NODE_NAME = "代码修复";
    private final CodeFixStreamPort codeFixStreamPort;

    /**
     * 创建节点动作
     */
    public AsyncNodeAction<MessagesState<String>> action() {
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
                // 构建修复请求
                String fixRequest = buildFixRequest(context);

                log.info("修复请求构建完成:\n{}", fixRequest);
                context.emitNodeMessage(NODE_NAME, "正在分析错误并修复代码...\n");

                Long appId = context.getAppId();
                CodeGenTypeEnum generationType = context.getGenerationType();

                // 如果没有设置代码生成类型，默认使用 VUE_PROJECT
                if (generationType == null) {
                    generationType = CodeGenTypeEnum.VUE_PROJECT;
                    context.setGenerationType(generationType);
                }

                Flux<String> fixStream = codeFixStreamPort.fixCodeStream(appId, generationType, fixRequest);
                CountDownLatch latch = new CountDownLatch(1);
                AtomicReference<Throwable> errorRef = new AtomicReference<>();

                // 节点仅消费标准消息流，TokenStream 回调细节由端口适配层统一封装。
                fixStream
                        .doOnNext(context::emit)
                        .doOnComplete(() -> {
                            log.info("代码修复完成");
                            latch.countDown();
                        })
                        .doOnError(error -> {
                            log.error("代码修复失败: {}", error.getMessage(), error);
                            errorRef.set(error);
                            latch.countDown();
                        })
                        .subscribe();

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
     * 包含：原始需求 + 构建错误信息 + 修复建议 + 输出格式指南
     * 错误信息来自真实的 npm run build 编译器输出
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

        // 添加构建错误信息
        request.append("## 构建错误（来自 npm run build 编译器输出）\n");
        if (qualityResult != null && CollUtil.isNotEmpty(qualityResult.getErrors())) {
            List<String> errors = qualityResult.getErrors();
            for (int i = 0; i < errors.size(); i++) {
                request.append(String.format("%d. %s\n", i + 1, errors.get(i)));
            }
        } else {
            request.append("- 构建未通过，请检查并修复编译错误\n");
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
     * Phase 1 阶段统一使用 VUE_PROJECT 的修复指南
     *
     * @param generationType 代码生成类型
     * @return 输出格式指南字符串
     */
    public static String getOutputFormatGuide(CodeGenTypeEnum generationType) {
        return switch (generationType) {
            case LEETCODE_PROJECT -> getLeetCodeFixGuide();
            default -> getVueFixGuide();
        };
    }

    private static String getVueFixGuide() {
        return """
                ## 修复指南（Vue 工程模式 - 构建错误修复）

                以上错误信息来自真实的 `npm run build` 编译器输出，请根据编译器给出的具体文件路径和错误信息进行针对性修复。

                ### 修复输出格式（必须遵守）

                #### 第一步：输出修复计划
                在开始修复之前，必须先分析所有错误，输出修复计划：

                📋 修复计划（共 N 个问题）：
                  1. [文件路径] - [错误简述]
                     - [修复操作描述]
                  2. [文件路径] - [错误简述]
                     - [修复操作描述]

                #### 第二步：按计划逐一修复
                📝 正在修复 [序号]/[总数]: [文件路径]
                📌 步骤 [序号].[子序号]/[序号].[子总数]: [修复操作描述]
                [使用工具执行修复]

                ### 修复步骤
                1. 仔细阅读编译器错误，定位出错的文件和行号
                2. 输出修复计划（格式见上方）
                3. 使用【文件读取工具】查看出错文件的内容
                4. 按计划逐一修复，每步输出进度信息
                5. 确保修复不会引入新的问题

                ### 禁止修改的文件
                以下文件是项目模板的基础设施文件，已经过验证，**严禁修改**：
                - `package.json`
                - `vite.config.js`
                - `src/main.js`
                - `index.html`

                ### 重要约束
                - **必须精准修复**：只修复当前构建报告中明确列出的具体错误
                - **禁止主动排查**：不要主动去检查或修复其他文件中可能存在的类似问题
                - 必须使用工具进行修复，不要直接输出代码块
                - 只修复编译器报告的具体错误，不要做无关的重构
                - 所有组件导入路径使用 `@` 别名（如 `@/components/Xxx.vue`）
                """;
    }

    private static String getLeetCodeFixGuide() {
        return """
                ## 修复指南（力扣题解模式 - 构建错误修复）

                以上错误信息来自真实的 `npm run build` 编译器输出，请根据编译器给出的具体文件路径和错误信息进行针对性修复。

                ### 修复输出格式（必须遵守）

                #### 第一步：输出修复计划
                📋 修复计划（共 N 个问题）：
                  1. [文件路径] - [错误简述]
                     - [修复操作描述]

                #### 第二步：按计划逐一修复
                📝 正在修复 [序号]/[总数]: [文件路径]
                [使用工具执行修复]

                ### 禁止修改的文件
                以下文件是项目模板的基础设施文件，已经过验证，**严禁修改**：
                - `package.json`
                - `vite.config.ts`
                - `tsconfig.json`
                - `env.d.ts`
                - `index.html`
                - `src/main.ts`
                - `src/App.vue`
                - `src/components/AnimationControls.vue`
                - `src/components/AnimationDemo.vue`
                - `src/components/CodePanel.vue`
                - `src/components/CompareTable.vue`
                - `src/components/CoreIdea.vue`
                - `src/components/ExplanationBox.vue`
                - `src/components/TabContainer.vue`
                - `src/composables/useAnimation.ts`
                - `src/styles/theme.css`
                - `src/types/index.ts`

                ### 重要约束
                - **必须精准修复**：只修复当前构建报告中明确列出的具体错误
                - **禁止主动排查**：不要主动去检查或修复其他文件中可能存在的类似问题
                - 必须使用工具进行修复，不要直接输出代码块
                - 只修复编译器报告的具体错误，不要做无关的重构
                - 只能修改 `src/data/` 和 `src/components/visualizations/` 下的文件
                """;
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
