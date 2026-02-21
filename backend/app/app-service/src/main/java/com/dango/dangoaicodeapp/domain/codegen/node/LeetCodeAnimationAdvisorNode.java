package com.dango.dangoaicodeapp.domain.codegen.node;

import com.dango.dangoaicodeapp.domain.codegen.ai.service.LeetCodeAnimationAdvisorService;
import com.dango.dangoaicodeapp.domain.codegen.workflow.state.WorkflowContext;
import com.dango.dangoaicodecommon.utils.SpringContextUtil;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * LeetCode 动画设计建议节点
 * 分析力扣题目，输出算法思路和动画设计建议，支持流式输出
 */
@Slf4j
@Component
public class LeetCodeAnimationAdvisorNode {

    private static final String NODE_NAME = "动画设计建议";

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: {}", NODE_NAME);

            context.emitNodeStart(NODE_NAME);
            context.restoreMonitorContext();
            context.emitNodeMessage(NODE_NAME, "正在分析题目并生成动画设计建议...\n");

            String userPrompt = context.getEnhancedPrompt();

            StreamingChatModel streamingModel = SpringContextUtil.getBean("odinaryStreamingChatModel", StreamingChatModel.class);
            LeetCodeAnimationAdvisorService advisor = AiServices.builder(LeetCodeAnimationAdvisorService.class)
                    .streamingChatModel(streamingModel)
                    .build();

            TokenStream tokenStream = advisor.advise(userPrompt);
            StringBuilder adviceBuilder = new StringBuilder();
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<Throwable> errorRef = new AtomicReference<>();

            tokenStream
                    .onPartialResponse(chunk -> {
                        adviceBuilder.append(chunk);
                        context.emit(chunk);
                    })
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

            String advice = adviceBuilder.toString();
            String enhanced = context.getEnhancedPrompt() +
                    "\n\n## 动画设计建议（由 AI 分析生成，请严格参考）\n" + advice;
            context.setEnhancedPrompt(enhanced);

            context.emitNodeMessage(NODE_NAME, "\n动画设计建议生成完成\n");
            context.emitNodeComplete(NODE_NAME);
            context.setCurrentStep(NODE_NAME);
            return WorkflowContext.saveContext(context);
        });
    }
}
