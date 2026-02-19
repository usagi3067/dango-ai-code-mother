package com.dango.dangoaicodeapp.domain.codegen.node;

import cn.hutool.core.io.FileUtil;
import com.dango.dangoaicodeapp.domain.app.valueobject.CodeGenTypeEnum;
import com.dango.dangoaicodeapp.domain.app.valueobject.OperationModeEnum;
import com.dango.dangoaicodeapp.domain.codegen.workflow.state.WorkflowContext;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 代码读取节点
 * 读取项目目录结构，为 AI 提供项目概览
 *
 * 复用 FileDirReadTool 的过滤逻辑和输出格式
 *
 * @author dango
 */
@Slf4j
@Component
public class CodeReaderNode {

    private static final String NODE_NAME = "代码读取";

    /**
     * 代码输出根目录
     */
    private static final String CODE_OUTPUT_ROOT_DIR = System.getProperty("user.dir") + "/tmp/code_output";

    /**
     * 需要忽略的文件和目录（与 FileDirReadTool 保持一致）
     */
    private static final Set<String> IGNORED_NAMES = Set.of(
            "node_modules", ".git", "dist", "build", ".DS_Store",
            ".env", "target", ".mvn", ".idea", ".vscode", "coverage"
    );

    /**
     * 需要忽略的文件扩展名（与 FileDirReadTool 保持一致）
     */
    private static final Set<String> IGNORED_EXTENSIONS = Set.of(
            ".log", ".tmp", ".cache", ".lock"
    );

    /**
     * 创建节点动作
     */
    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: {}", NODE_NAME);

            // 发送节点开始消息
            context.emitNodeStart(NODE_NAME);
            context.emitNodeMessage(NODE_NAME, "正在读取项目结构...\n");

            Long appId = context.getAppId();
            CodeGenTypeEnum generationType = context.getGenerationType();

            // 读取项目目录结构
            String projectStructure = readProjectStructure(appId, generationType);

            if (projectStructure == null || projectStructure.isEmpty()) {
                // 没有现有项目，切换到创建模式
                context.setOperationMode(OperationModeEnum.CREATE);
                log.warn("未找到现有项目，切换到创建模式");
                context.emitNodeMessage(NODE_NAME, "未找到现有项目代码，将切换到创建模式\n");
            } else {
                context.setProjectStructure(projectStructure);
                log.info("项目结构读取完成:\n{}", projectStructure);
                context.emitNodeMessage(NODE_NAME, "项目结构读取完成\n");
            }

            // 发送节点完成消息
            context.emitNodeComplete(NODE_NAME);

            // 更新状态
            context.setCurrentStep(NODE_NAME);
            return WorkflowContext.saveContext(context);
        });
    }

    /**
     * 读取项目目录结构
     * 复用 FileDirReadTool 的逻辑和输出格式
     *
     * @param appId 应用 ID
     * @param codeGenType 代码生成类型
     * @return 目录结构字符串，如果项目不存在则返回 null
     */
    public static String readProjectStructure(Long appId, CodeGenTypeEnum codeGenType) {
        if (appId == null || appId <= 0) {
            log.warn("无效的 appId: {}", appId);
            return null;
        }

        Path projectPath = getProjectPath(appId, codeGenType);
        if (projectPath == null) {
            log.info("未找到 appId={} 的项目目录", appId);
            return null;
        }

        File targetDir = projectPath.toFile();
        if (!targetDir.exists() || !targetDir.isDirectory()) {
            return null;
        }

        // 复用 FileDirReadTool 的输出格式
        StringBuilder structure = new StringBuilder();
        structure.append("项目目录结构:\n");

        // 使用 Hutool 递归获取所有文件，应用过滤规则
        // 注意：过滤器需要同时过滤目录和文件，防止进入 node_modules 等目录
        List<File> allFiles = FileUtil.loopFiles(targetDir, file -> {
            // 检查文件本身是否应该忽略
            if (shouldIgnore(file.getName())) {
                return false;
            }
            // 检查文件的父目录链是否包含应该忽略的目录
            File parent = file.getParentFile();
            while (parent != null && !parent.equals(targetDir)) {
                if (shouldIgnore(parent.getName())) {
                    return false;
                }
                parent = parent.getParentFile();
            }
            return true;
        });

        // 按路径排序显示，使用相对路径格式
        allFiles.stream()
                .sorted((f1, f2) -> f1.getPath().compareTo(f2.getPath()))
                .forEach(file -> {
                    // 获取相对路径
                    String relativePath = targetDir.toPath().relativize(file.toPath()).toString();
                    structure.append("- ").append(relativePath).append("\n");
                });

        return structure.toString();
    }

    /**
     * 获取项目根目录路径
     *
     * @param appId 应用 ID
     * @param codeGenType 代码生成类型
     * @return 项目根目录路径，如果不存在则返回 null
     */
    public static Path getProjectPath(Long appId, CodeGenTypeEnum codeGenType) {
        if (appId == null || appId <= 0) {
            return null;
        }

        String dirName = (codeGenType != null ? codeGenType.getValue() : "vue_project") + "_" + appId;
        Path projectPath = Paths.get(CODE_OUTPUT_ROOT_DIR, dirName);
        if (Files.exists(projectPath)) {
            return projectPath;
        }

        return null;
    }
    /**
     * 判断是否应该忽略该文件或目录（与 FileDirReadTool 一致）
     */
    public static boolean shouldIgnore(String fileName) {
        if (IGNORED_NAMES.contains(fileName)) {
            return true;
        }
        return IGNORED_EXTENSIONS.stream().anyMatch(fileName::endsWith);
    }
}
