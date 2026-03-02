package com.dango.dangoaicodeapp.domain.codegen.tools;

import com.dango.dangoaicodeapp.domain.app.valueobject.CodeGenTypeEnum;
import com.dango.dangoaicodeapp.model.constant.AppConstant;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 代码生成场景的工具基类
 *
 * <p>继承 ai 模块的 BaseTool，添加代码生成特定的能力。
 */
public abstract class CodeGenBaseTool extends com.dango.aicodegenerate.tool.BaseTool {

    /**
     * 根据 appId 获取项目根目录路径
     * 自动探测项目类型（vue_project、leetcode_project 等）
     *
     * @param appId 应用 ID
     * @return 项目根目录路径，如果不存在则返回 null
     */
    protected Path getProjectRoot(Long appId) {
        if (appId == null || appId <= 0) {
            return null;
        }

        for (CodeGenTypeEnum type : CodeGenTypeEnum.values()) {
            String dirName = type.getValue() + "_" + appId;
            Path projectPath = Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR, dirName);
            if (Files.exists(projectPath)) {
                return projectPath;
            }
        }

        return null;
    }

    /**
     * 根据 appId 获取项目根目录路径，如果不存在则使用默认类型创建路径
     *
     * @param appId 应用 ID
     * @param defaultType 默认的代码生成类型（用于新项目）
     * @return 项目根目录路径
     */
    protected Path getProjectRootOrDefault(Long appId, CodeGenTypeEnum defaultType) {
        Path existingPath = getProjectRoot(appId);
        if (existingPath != null) {
            return existingPath;
        }

        if (defaultType == null) {
            defaultType = CodeGenTypeEnum.VUE_PROJECT;
        }
        String dirName = defaultType.getValue() + "_" + appId;
        return Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR, dirName);
    }
}
