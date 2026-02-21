package com.dango.dangoaicodeapp.domain.codegen.node;

import com.dango.dangoaicodeapp.domain.codegen.ai.factory.AiQAServiceFactory;
import com.dango.dangoaicodeapp.domain.codegen.ai.service.QAService;
import com.dango.dangoaicodeapp.domain.codegen.workflow.state.WorkflowContext;
import com.dango.dangoaicodecommon.utils.SpringContextUtil;
import dev.langchain4j.service.TokenStream;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 问答节点
 * 回答用户关于项目代码和技术的问题，支持流式输出
 */
@Slf4j
@Component
public class QANode {

    private static final String NODE_NAME = "问答";

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: {}", NODE_NAME);

            context.emitNodeStart(NODE_NAME);
            context.restoreMonitorContext();

            String userInput = context.getOriginalPrompt();
            String projectStructure = context.getProjectStructure();
            String qaInput = String.format(
                "## 项目结构\n%s\n\n## 用户问题\n%s",
                projectStructure != null ? projectStructure : "无项目结构信息",
                userInput
            );

            AiQAServiceFactory factory = SpringContextUtil.getBean(AiQAServiceFactory.class);
            QAService qaService = factory.createService();

            TokenStream tokenStream = qaService.answer(qaInput);
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<Throwable> errorRef = new AtomicReference<>();

            tokenStream
                    .onPartialResponse(chunk -> context.emit(chunk))
                    .onCompleteResponse(response -> latch.countDown())
                    .onError(error -> {
                        errorRef.set(error);
                        latch.countDown();
                    })
                    .start();

            latch.await();

            if (errorRef.get() != null) {
                throw new RuntimeException(errorRef.get());
            }

            context.emitNodeMessage(NODE_NAME, "\n回答完成\n");
            context.emitNodeComplete(NODE_NAME);
            context.setCurrentStep(NODE_NAME);
            return WorkflowContext.saveContext(context);
        });
    }
}
