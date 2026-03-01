package com.dango.dangoaicodeapp.infrastructure.config;

import cn.hutool.core.thread.ExecutorBuilder;
import cn.hutool.core.thread.ThreadFactoryBuilder;
import com.dango.dangoaicodeapp.domain.codegen.port.WorkflowStreamPort;
import com.dango.dangoaicodeapp.domain.codegen.workflow.CodeGenWorkflow;
import com.dango.dangoaicodeapp.domain.codegen.workflow.CodeGenWorkflowFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 代码生成工作流配置。
 * 统一托管 Workflow 生命周期，避免请求级重复创建并行执行器。
 */
@Configuration
public class CodeGenWorkflowConfig {

    @Bean(name = "codeGenWorkflowParallelExecutor", destroyMethod = "shutdown")
    public ExecutorService codeGenWorkflowParallelExecutor() {
        return ExecutorBuilder.create()
                .setCorePoolSize(10)
                .setMaxPoolSize(20)
                .setWorkQueue(new LinkedBlockingQueue<>(100))
                .setThreadFactory(ThreadFactoryBuilder.create()
                        .setNamePrefix("Parallel-Image-Collect-")
                        .build())
                .build();
    }

    @Bean
    public CodeGenWorkflow codeGenWorkflow(
            @Qualifier("codeGenWorkflowParallelExecutor") ExecutorService parallelExecutor,
            CodeGenWorkflowFactory codeGenWorkflowFactory,
            WorkflowStreamPort workflowStreamPort) {
        return new CodeGenWorkflow(parallelExecutor, codeGenWorkflowFactory, workflowStreamPort);
    }
}
