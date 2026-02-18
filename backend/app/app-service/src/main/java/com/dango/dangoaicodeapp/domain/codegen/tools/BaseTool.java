package com.dango.dangoaicodeapp.domain.codegen.tools;

import cn.hutool.json.JSONObject;
import com.dango.dangoaicodeapp.model.constant.AppConstant;
import com.dango.dangoaicodeapp.model.enums.CodeGenTypeEnum;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 工具基类
 * 定义所有工具的通用接口
 */
public abstract class BaseTool {

    /**
     * 获取工具的英文名称（对应方法名）
     *
     * @return 工具英文名称
     */
    public abstract String getToolName();

    /**
     * 获取工具的中文显示名称
     *
     * @return 工具中文名称
     */
    public abstract String getDisplayName();

    /**
     * 生成工具请求时的返回值（显示给用户）
     *
     * @return 工具请求显示内容
     */
    public String generateToolRequestResponse() {
        return String.format("\n\n[选择工具] %s\n\n", getDisplayName());
    }

    /**
     * 生成工具执行结果格式（保存到数据库）
     *
     * @param arguments 工具执行参数
     * @return 格式化的工具执行结果
     */
    public abstract String generateToolExecutedResult(JSONObject arguments);

    /**
     * 根据 appId 获取项目根目录路径
     * 自动探测项目类型（html、multi_file、vue_project）
     *
     * @param appId 应用 ID
     * @return 项目根目录路径，如果不存在则返回 null
     */
    protected Path getProjectRoot(Long appId) {
        if (appId == null || appId <= 0) {
            return null;
        }

        // 按优先级尝试查找各种类型的项目目录
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

        // 如果项目不存在，使用默认类型
        if (defaultType == null) {
            defaultType = CodeGenTypeEnum.VUE_PROJECT;
        }
        String dirName = defaultType.getValue() + "_" + appId;
        return Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR, dirName);
    }
}
