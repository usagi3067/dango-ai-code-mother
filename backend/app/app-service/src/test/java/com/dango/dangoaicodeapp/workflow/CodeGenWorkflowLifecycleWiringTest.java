package com.dango.dangoaicodeapp.workflow;

import com.dango.dangoaicodeapp.application.service.impl.CodeGenApplicationServiceImpl;
import com.dango.dangoaicodeapp.application.service.impl.CodeGenWorkflowExecutor;
import com.dango.dangoaicodeapp.domain.codegen.workflow.CodeGenWorkflowFactory;
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

        assertEquals(2, workflowBeanMethod.getParameterCount(),
                "codeGenWorkflow bean should receive managed executor and workflow factory");
        assertEquals(ExecutorService.class, workflowBeanMethod.getParameterTypes()[0],
                "codeGenWorkflow should depend on ExecutorService abstraction");
        assertEquals(CodeGenWorkflowFactory.class, workflowBeanMethod.getParameterTypes()[1],
                "codeGenWorkflow should depend on workflow factory abstraction");

        boolean hasWorkflowField = Arrays.stream(CodeGenApplicationServiceImpl.class.getDeclaredFields())
                .anyMatch(field -> field.getType().equals(CodeGenWorkflowExecutor.class));

        assertTrue(hasWorkflowField,
                "CodeGenApplicationServiceImpl should inject and reuse CodeGenWorkflowExecutor bean");
    }
}
