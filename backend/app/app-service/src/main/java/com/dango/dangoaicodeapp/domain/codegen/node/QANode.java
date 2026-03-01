package com.dango.dangoaicodeapp.domain.codegen.node;

import cn.hutool.json.JSONUtil;
import com.dango.aicodegenerate.model.message.AiResponseMessage;
import com.dango.dangoaicodeapp.domain.codegen.port.QaGateway;
import com.dango.dangoaicodeapp.domain.codegen.workflow.state.WorkflowContext;
import dev.langchain4j.service.TokenStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 问答节点。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QANode {

    private static final String NODE_NAME = "问答";

    private final QaGateway qaGateway;

    public AsyncNodeAction<MessagesState<String>> action() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: {}", NODE_NAME);

            context.emitNodeStart(NODE_NAME);

            String userInput = context.getOriginalPrompt();
            String projectStructure = context.getProjectStructure();
            String qaInput = String.format(
                "## 项目结构\n%s\n\n## 用户问题\n%s",
                projectStructure != null ? projectStructure : "无项目结构信息",
                userInput
            );

            TokenStream tokenStream = qaGateway.answer(context.getAppId(), qaInput);
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<Throwable> errorRef = new AtomicReference<>();

            tokenStream
                    .onPartialResponse(chunk -> context.emit(JSONUtil.toJsonStr(new AiResponseMessage(chunk))))
                    .onCompleteResponse(response -> latch.countDown())
                    .onError(error -> {
                        errorRef.set(error);
                        latch.countDown();
                    })
                    .start();

            latch.await(10, TimeUnit.MINUTES);

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
