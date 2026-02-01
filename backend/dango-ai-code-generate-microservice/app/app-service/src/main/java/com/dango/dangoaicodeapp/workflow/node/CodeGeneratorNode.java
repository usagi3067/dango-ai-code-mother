package com.dango.dangoaicodeapp.workflow.node;

import com.dango.dangoaicodeapp.core.AiCodeGeneratorFacade;
import com.dango.dangoaicodeapp.model.enums.CodeGenTypeEnum;
import com.dango.dangoaicodeapp.workflow.state.WorkflowContext;
import com.dango.dangoaicodecommon.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 代码生成节点
 * 调用 AI 服务生成代码并保存，支持流式输出
 */
@Slf4j
@Component
public class CodeGeneratorNode {

    private static final String NODE_NAME = "代码生成";

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: {}", NODE_NAME);

            // 发送节点开始消息（使用 context 而非 ThreadLocal）
            context.emitNodeStart(NODE_NAME);

            String enhancedPrompt = context.getEnhancedPrompt();
            CodeGenTypeEnum generationType = context.getGenerationType();
            Long appId = context.getAppId();

            context.emitNodeMessage(NODE_NAME, 
                    String.format("开始生成 %s 类型代码...\n", generationType.getText()));

            try {
                // 获取 AI 代码生成外观服务
                AiCodeGeneratorFacade codeGeneratorFacade = SpringContextUtil.getBean(AiCodeGeneratorFacade.class);
                
                // 使用流式生成，实时输出代码内容
                Flux<String> codeStream = codeGeneratorFacade.generateAndSaveCodeStream(
                        enhancedPrompt, generationType, appId);
                
                // 使用 CountDownLatch 等待流式生成完成
                CountDownLatch latch = new CountDownLatch(1);
                AtomicReference<Throwable> errorRef = new AtomicReference<>();
                
                // 订阅流，实时输出到前端
                // 使用 subscribeOn(Schedulers.immediate()) 确保在当前线程执行
                codeStream
                        .subscribeOn(Schedulers.immediate())
                        .doOnNext(chunk -> {
                            // 实时输出代码片段到前端（使用 context 而非 ThreadLocal）
                            context.emit(chunk);
                        })
                        .doOnError(error -> {
                            errorRef.set(error);
                        })
                        .doFinally(signalType -> {
                            latch.countDown();
                        })
                        .subscribe();
                
                // 等待流式生成完成
                latch.await();
                
                // 检查是否有错误
                if (errorRef.get() != null) {
                    throw new RuntimeException(errorRef.get());
                }
                
                // 构建生成的代码目录路径
                String generatedCodeDir = buildGeneratedCodeDir(generationType, appId);
                context.setGeneratedCodeDir(generatedCodeDir);
                
                log.info("代码生成完成，目录: {}", generatedCodeDir);
                context.emitNodeMessage(NODE_NAME, "\n代码生成并保存完成\n");
                
            } catch (Exception e) {
                log.error("代码生成失败: {}", e.getMessage(), e);
                context.setErrorMessage("代码生成失败: " + e.getMessage());
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
     * 构建生成的代码目录路径
     */
    private static String buildGeneratedCodeDir(CodeGenTypeEnum generationType, Long appId) {
        // 与 CodeFileSaverExecutor 保持一致的目录命名规则
        String baseDir = System.getProperty("user.dir") + File.separator + "tmp" + File.separator + "code_output";
        String dirName = generationType.getValue() + "_" + appId;
        return baseDir + File.separator + dirName;
    }
}
