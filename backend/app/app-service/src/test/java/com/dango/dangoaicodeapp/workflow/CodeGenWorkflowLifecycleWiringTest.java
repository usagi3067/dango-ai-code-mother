package com.dango.dangoaicodeapp.workflow;

import com.dango.dangoaicodeapp.application.service.impl.CodeGenApplicationServiceImpl;
import com.dango.dangoaicodeapp.domain.codegen.workflow.CodeGenWorkflow;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodeGenWorkflowLifecycleWiringTest {

    @Test
    void shouldProvideManagedParallelExecutorBean() throws Exception {
        Class<?> configClass = Class.forName(
                "com.dango.dangoaicodeapp.infrastructure.config.CodeGenWorkflowConfig");

        Method executorBeanMethod = Arrays.stream(configClass.getDeclaredMethods())
                .filter(method -> method.getName().equals("codeGenWorkflowParallelExecutor"))
                .findFirst()
                .orElseThrow();

        Bean bean = executorBeanMethod.getAnnotation(Bean.class);
        assertNotNull(bean, "codeGenWorkflowParallelExecutor method should be annotated with @Bean");
        assertEquals("shutdown", bean.destroyMethod(),
                "parallel executor should be shutdown by Spring at container stop");
    }

    @Test
    void workflowShouldUseInjectedExecutorThroughConfig() throws Exception {
        Class<?> configClass = Class.forName(
                "com.dango.dangoaicodeapp.infrastructure.config.CodeGenWorkflowConfig");

        Method workflowBeanMethod = Arrays.stream(configClass.getDeclaredMethods())
                .filter(method -> method.getName().equals("codeGenWorkflow"))
                .findFirst()
                .orElseThrow();

        assertEquals(1, workflowBeanMethod.getParameterCount(),
                "codeGenWorkflow bean should receive a managed executor");
        assertEquals(ExecutorService.class, workflowBeanMethod.getParameterTypes()[0],
                "codeGenWorkflow should depend on ExecutorService abstraction");

        boolean hasWorkflowField = Arrays.stream(CodeGenApplicationServiceImpl.class.getDeclaredFields())
                .anyMatch(field -> field.getType().equals(CodeGenWorkflow.class));

        assertTrue(hasWorkflowField,
                "CodeGenApplicationServiceImpl should inject and reuse CodeGenWorkflow bean");
    }
}
