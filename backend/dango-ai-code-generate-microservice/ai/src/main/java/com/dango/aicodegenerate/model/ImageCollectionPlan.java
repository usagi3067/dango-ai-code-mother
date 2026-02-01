package com.dango.aicodegenerate.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 图片收集计划
 * AI 分析用户需求后生成的图片收集任务计划
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageCollectionPlan implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 内容图片搜索任务列表
     */
    private List<ImageSearchTask> contentImageTasks;

    /**
     * 插画图片搜索任务列表
     */
    private List<IllustrationTask> illustrationTasks;

    /**
     * 架构图生成任务列表
     */
    private List<DiagramTask> diagramTasks;

    /**
     * Logo 生成任务列表
     */
    private List<LogoTask> logoTasks;

    /**
     * 内容图片搜索任务
     */
    public record ImageSearchTask(String query, String description) implements Serializable {
    }

    /**
     * 插画搜索任务
     */
    public record IllustrationTask(String query, String description) implements Serializable {
    }

    /**
     * 架构图生成任务
     */
    public record DiagramTask(String mermaidCode, String description) implements Serializable {
    }

    /**
     * Logo 生成任务
     */
    public record LogoTask(String description) implements Serializable {
    }
}
