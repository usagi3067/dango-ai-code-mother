package com.dango.dangoaicodeapp.infrastructure.ai.config;

import com.dango.aicodegenerate.tool.ToolConfig;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 代码生成场景的工具配置
 *
 * <p>实现 ai 模块的 ToolConfig 接口，提供代码生成场景特定的工具配置。
 */
@Component
public class CodeGenToolConfig implements ToolConfig {

    private static final Map<String, String> TRIGGER_PARAMS = Map.of(
        "writeFile", "relativeFilePath",
        "modifyFile", "relativeFilePath",
        "readFile", "relativeFilePath",
        "readDir", "relativeDirPath",
        "deleteFile", "relativeFilePath",
        "searchContentImages", "query",
        "searchIllustrations", "query",
        "generateLogos", "description",
        "generateMermaidDiagram", "mermaidCode"
    );

    @Override
    public String getTriggerParam(String toolName) {
        return TRIGGER_PARAMS.get(toolName);
    }

    @Override
    public String getAction(String toolName) {
        return switch (toolName) {
            case "writeFile" -> "write";
            case "modifyFile" -> "modify";
            case "readFile", "readDir" -> "read";
            case "deleteFile" -> "delete";
            case "searchContentImages", "searchIllustrations" -> "search";
            case "generateLogos", "generateMermaidDiagram" -> "generate";
            default -> "unknown";
        };
    }
}
