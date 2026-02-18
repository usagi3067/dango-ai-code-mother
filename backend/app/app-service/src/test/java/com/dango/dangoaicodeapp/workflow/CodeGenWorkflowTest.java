package com.dango.dangoaicodeapp.workflow;

import com.dango.dangoaicodeapp.DangoAiCodeAppApplication;
import com.dango.dangoaicodeapp.domain.codegen.workflow.CodeGenWorkflow;
import com.dango.dangoaicodeapp.domain.codegen.workflow.state.WorkflowContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = DangoAiCodeAppApplication.class,
        properties = {
                "dubbo.consumer.check=false",           // 禁用启动时检查
                "dubbo.registry.check=false",           // 禁用注册中心检查
                "dubbo.reference.check=false"           // 禁用引用检查
        })
class CodeGenWorkflowTest {

    @Test
    void testTechBlogWorkflow() {
        CodeGenWorkflow workflow = new CodeGenWorkflow();
        try {
            WorkflowContext result = workflow.executeWorkflow("创建一个技术博客网站，需要展示编程教程和系统架构");
            Assertions.assertNotNull(result);
            System.out.println("生成类型: " + result.getGenerationType());
            System.out.println("生成的代码目录: " + result.getGeneratedCodeDir());
            System.out.println("构建结果目录: " + result.getBuildResultDir());
            System.out.println("收集的图片数量: " + (result.getImageList() != null ? result.getImageList().size() : 0));
        } finally {
            workflow.shutdown();
        }
    }

    @Test
    void testCorporateWorkflow() {
        CodeGenWorkflow workflow = new CodeGenWorkflow();
        try {
            WorkflowContext result = workflow.executeWorkflow("创建企业官网，展示公司形象和业务介绍");
            Assertions.assertNotNull(result);
            System.out.println("生成类型: " + result.getGenerationType());
            System.out.println("生成的代码目录: " + result.getGeneratedCodeDir());
            System.out.println("构建结果目录: " + result.getBuildResultDir());
            System.out.println("收集的图片数量: " + (result.getImageList() != null ? result.getImageList().size() : 0));
        } finally {
            workflow.shutdown();
        }
    }

    @Test
    void testVueProjectWorkflow() {
        CodeGenWorkflow workflow = new CodeGenWorkflow();
        try {
            WorkflowContext result = workflow.executeWorkflow("创建一个Vue前端项目，包含用户管理和数据展示功能");
            Assertions.assertNotNull(result);
            System.out.println("生成类型: " + result.getGenerationType());
            System.out.println("生成的代码目录: " + result.getGeneratedCodeDir());
            System.out.println("构建结果目录: " + result.getBuildResultDir());
            System.out.println("收集的图片数量: " + (result.getImageList() != null ? result.getImageList().size() : 0));
        } finally {
            workflow.shutdown();
        }
    }

    @Test
    void testSimpleHtmlWorkflow() {
        CodeGenWorkflow workflow = new CodeGenWorkflow();
        try {
            WorkflowContext result = workflow.executeWorkflow("创建一个简单的个人主页");
            Assertions.assertNotNull(result);
            System.out.println("生成类型: " + result.getGenerationType());
            System.out.println("生成的代码目录: " + result.getGeneratedCodeDir());
            System.out.println("构建结果目录: " + result.getBuildResultDir());
            System.out.println("收集的图片数量: " + (result.getImageList() != null ? result.getImageList().size() : 0));
        } finally {
            workflow.shutdown();
        }
    }

    @Test
    void testEcommerceWorkflow() {
        CodeGenWorkflow workflow = new CodeGenWorkflow();
        try {
            WorkflowContext result = workflow.executeWorkflow("创建一个电子商务网站，需要商品展示、购物车和支付功能");
            Assertions.assertNotNull(result);
            System.out.println("生成类型: " + result.getGenerationType());
            System.out.println("生成的代码目录: " + result.getGeneratedCodeDir());
            System.out.println("收集的图片数量: " + (result.getImageList() != null ? result.getImageList().size() : 0));
        } finally {
            workflow.shutdown();
        }
    }
}
