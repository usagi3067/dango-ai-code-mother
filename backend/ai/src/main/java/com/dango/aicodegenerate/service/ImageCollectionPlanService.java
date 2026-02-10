package com.dango.aicodegenerate.service;

import com.dango.aicodegenerate.model.ImageCollectionPlan;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * 图片收集计划 AI 服务接口
 * 使用 AI 分析用户需求，生成图片收集计划
 */
public interface ImageCollectionPlanService {

    /**
     * 根据用户提示词生成图片收集计划
     * AI 会分析需求并规划需要收集的各类图片任务
     */
    @SystemMessage(fromResource = "prompt/image-collection-plan-system-prompt.txt")
    ImageCollectionPlan planImageCollection(@UserMessage String userPrompt);
}
