package com.dango.dangoaicodeapp.domain.codegen.tools;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 工具管理器
 *
 * <p>统一管理所有工具，提供根据名称获取工具的功能。
 * <p>实现 ai 模块的 ToolRegistry 接口。
 */
@Slf4j
@Component
public class ToolManager implements com.dango.aicodegenerate.tool.ToolRegistry {

    /**
     * 工具名称到工具实例的映射
     */
    private final Map<String, com.dango.aicodegenerate.tool.BaseTool> toolMap = new HashMap<>();

    /**
     * 自动注入所有工具
     */
    @Resource
    private com.dango.aicodegenerate.tool.BaseTool[] tools;

    /**
     * 初始化工具映射
     */
    @PostConstruct
    public void initTools() {
        for (com.dango.aicodegenerate.tool.BaseTool tool : tools) {
            registerTool(tool);
        }
        log.info("工具管理器初始化完成，共注册 {} 个工具", toolMap.size());
    }

    @Override
    public com.dango.aicodegenerate.tool.BaseTool getTool(String toolName) {
        return toolMap.get(toolName);
    }

    @Override
    public com.dango.aicodegenerate.tool.BaseTool[] getAllTools() {
        return tools;
    }

    @Override
    public void registerTool(com.dango.aicodegenerate.tool.BaseTool tool) {
        toolMap.put(tool.getToolName(), tool);
        log.info("注册工具: {} -> {}", tool.getToolName(), tool.getDisplayName());
    }
}
