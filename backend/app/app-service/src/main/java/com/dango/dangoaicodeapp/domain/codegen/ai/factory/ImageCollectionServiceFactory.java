package com.dango.dangoaicodeapp.domain.codegen.ai.factory;

import com.dango.dangoaicodeapp.domain.codegen.tools.ImageSearchTool;
import com.dango.dangoaicodeapp.domain.codegen.tools.LogoGeneratorTool;
import com.dango.dangoaicodeapp.domain.codegen.tools.MermaidDiagramTool;
import com.dango.dangoaicodeapp.domain.codegen.tools.UndrawIllustrationTool;
import com.dango.dangoaicodeapp.domain.codegen.ai.service.ImageCollectionPlanService;
import com.dango.dangoaicodeapp.domain.codegen.ai.service.ImageCollectionService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class ImageCollectionServiceFactory {

    @Resource
    private ChatModel chatModel;

    @Resource
    private ImageSearchTool imageSearchTool;

    @Resource
    private UndrawIllustrationTool undrawIllustrationTool;

    @Resource
    private MermaidDiagramTool mermaidDiagramTool;

    @Resource
    private LogoGeneratorTool logoGeneratorTool;

    /**
     * 创建图片收集 AI 服务
     */
    @Bean
    public ImageCollectionService createImageCollectionService() {
        return AiServices.builder(ImageCollectionService.class)
                .chatModel(chatModel)
                .tools(
                        imageSearchTool,
                        undrawIllustrationTool,
                        mermaidDiagramTool,
                        logoGeneratorTool
                )
                .build();
    }

    /**
     * 创建图片收集计划 AI 服务
     * 用于分析用户需求并生成图片收集计划，为并发执行做准备
     */
    @Bean
    public ImageCollectionPlanService createImageCollectionPlanService() {
        return AiServices.builder(ImageCollectionPlanService.class)
                .chatModel(chatModel)
                .build();
    }
}
