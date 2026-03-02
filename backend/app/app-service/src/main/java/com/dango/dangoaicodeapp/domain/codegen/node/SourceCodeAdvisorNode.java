package com.dango.dangoaicodeapp.domain.codegen.node;

import cn.hutool.json.JSONUtil;
import com.dango.aicodegenerate.model.message.AiResponseMessage;
import com.dango.dangoaicodeapp.domain.codegen.port.AnimationAdvisorStreamPort;
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
 * 源码讲解规划节点。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SourceCodeAdvisorNode {

    private static final String NODE_NAME = "源码讲解规划";

    private final WorkflowMessagePort workflowMessagePort;
    private final AnimationAdvisorStreamPort animationAdvisorStreamPort;

    public AsyncNodeAction<MessagesState<String>> action() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: {}", NODE_NAME);

            workflowMessagePort.emitNodeStart(context.getWorkflowExecutionId(), NODE_NAME);
            workflowMessagePort.emitNodeMessage(context.getWorkflowExecutionId(), NODE_NAME, "正在分析面试问题并规划源码讲解策略...\n");

            String userPrompt = context.getOriginalPrompt();

            Flux<String> adviseStream = animationAdvisorStreamPort.adviseInterviewSourceCode(userPrompt);
            StringBuilder adviceBuilder = new StringBuilder();
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<Throwable> errorRef = new AtomicReference<>();

            adviseStream
                    .doOnNext(chunk -> {
                        adviceBuilder.append(chunk);
                        workflowMessagePort.emitRaw(context.getWorkflowExecutionId(), JSONUtil.toJsonStr(new AiResponseMessage(chunk)));
                    })
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

            if (adviceBuilder.isEmpty()) {
                throw new RuntimeException("源码讲解规划生成超时");
            }

            context.setEnhancedPrompt(adviceBuilder.toString());
            workflowMessagePort.emitNodeMessage(context.getWorkflowExecutionId(), NODE_NAME, "\n源码讲解规划生成完成\n");
            workflowMessagePort.emitNodeComplete(context.getWorkflowExecutionId(), NODE_NAME);
            context.setCurrentStep(NODE_NAME);
            return WorkflowContext.saveContext(context);
        });
    }
}
