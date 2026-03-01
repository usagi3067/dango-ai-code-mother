package com.dango.dangoaicodeapp.domain.codegen.node;

import com.dango.dangoaicodeapp.domain.codegen.port.QaStreamPort;
import com.dango.dangoaicodeapp.domain.codegen.port.WorkflowMessagePort;
import com.dango.dangoaicodeapp.domain.codegen.workflow.state.WorkflowContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

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

    private final WorkflowMessagePort workflowMessagePort;
    private final QaStreamPort qaStreamPort;

    public AsyncNodeAction<MessagesState<String>> action() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: {}", NODE_NAME);

            workflowMessagePort.emitNodeStart(context.getWorkflowExecutionId(), NODE_NAME);

            String userInput = context.getOriginalPrompt();
            String projectStructure = context.getProjectStructure();
            String qaInput = String.format(
                "## 项目结构\n%s\n\n## 用户问题\n%s",
                projectStructure != null ? projectStructure : "无项目结构信息",
                userInput
            );

            Flux<String> answerStream = qaStreamPort.answer(context.getAppId(), qaInput);
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<Throwable> errorRef = new AtomicReference<>();

            answerStream
                    .doOnNext(chunk ->
                            workflowMessagePort.emitRaw(context.getWorkflowExecutionId(), chunk))
                    .doOnComplete(latch::countDown)
                    .doOnError(error -> {
                        errorRef.set(error);
                        latch.countDown();
                    })
                    .subscribe();

            latch.await(10, TimeUnit.MINUTES);

            if (errorRef.get() != null) {
                throw new RuntimeException(errorRef.get());
            }

            workflowMessagePort.emitNodeMessage(context.getWorkflowExecutionId(), NODE_NAME, "\n回答完成\n");
            workflowMessagePort.emitNodeComplete(context.getWorkflowExecutionId(), NODE_NAME);
            context.setCurrentStep(NODE_NAME);
            return WorkflowContext.saveContext(context);
        });
    }
}
