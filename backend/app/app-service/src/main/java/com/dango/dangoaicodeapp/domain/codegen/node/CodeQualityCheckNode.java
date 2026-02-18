package com.dango.dangoaicodeapp.domain.codegen.node;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.dango.aicodegenerate.model.QualityResult;
import com.dango.dangoaicodeapp.domain.codegen.ai.service.CodeQualityCheckService;
import com.dango.dangoaicodeapp.domain.codegen.workflow.state.WorkflowContext;
import com.dango.dangoaicodecommon.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 代码质量检查节点
 * 读取生成的代码文件，调用 AI 进行质量检查
 */
@Slf4j
@Component
public class CodeQualityCheckNode {

    private static final String NODE_NAME = "代码质量检查";

    /**
     * 需要检查的文件扩展名
     */
    private static final List<String> CODE_EXTENSIONS = Arrays.asList(
            ".html", ".htm", ".css", ".js", ".json", ".vue", ".ts", ".jsx", ".tsx"
    );

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: {}", NODE_NAME);

            context.emitNodeStart(NODE_NAME);

            String generatedCodeDir = context.getGeneratedCodeDir();
            QualityResult qualityResult;

            try {
                // 恢复监控上下文到当前线程（用于跨线程传递监控信息）
                context.restoreMonitorContext();

                // 1. 读取并拼接代码文件内容
                String codeContent = readAndConcatenateCodeFiles(generatedCodeDir);

                if (StrUtil.isBlank(codeContent)) {
                    log.warn("未找到可检查的代码文件");
                    qualityResult = QualityResult.builder()
                            .isValid(false)
                            .errors(List.of("未找到可检查的代码文件"))
                            .suggestions(List.of("请确保代码生成成功"))
                            .build();
                } else {
                    context.emitNodeMessage(NODE_NAME, "正在分析代码质量...\n");

                    // 2. 调用 AI 进行代码质量检查
                    // 不需要转义 {{}}，因为我们直接使用 ChatModel，不经过模板解析
                    CodeQualityCheckService qualityCheckService = SpringContextUtil.getBean(CodeQualityCheckService.class);
                    qualityResult = qualityCheckService.checkCodeQuality(codeContent);

                    log.info("代码质量检查完成 - 是否通过: {}", qualityResult.getIsValid());

                    // 输出检查结果
                    if (qualityResult.getIsValid()) {
                        context.emitNodeMessage(NODE_NAME, "✅ 代码质量检查通过\n");
                    } else {
                        context.emitNodeMessage(NODE_NAME, "❌ 代码质量检查未通过\n");
                        if (qualityResult.getErrors() != null && !qualityResult.getErrors().isEmpty()) {
                            context.emitNodeMessage(NODE_NAME, "发现问题:\n");
                            qualityResult.getErrors().forEach(error ->
                                    context.emitNodeMessage(NODE_NAME, "  - " + error + "\n"));
                        }
                    }
                }
            } catch (Exception e) {
                log.error("代码质量检查异常: {}", e.getMessage(), e);
                // 异常时直接跳到下一个步骤，避免阻塞流程
                qualityResult = QualityResult.builder()
                        .isValid(true)
                        .build();
                context.emitNodeError(NODE_NAME, e.getMessage());
            }

            context.emitNodeComplete(NODE_NAME);

            // 3. 更新状态
            context.setCurrentStep(NODE_NAME);
            context.setQualityResult(qualityResult);
            return WorkflowContext.saveContext(context);
        });
    }

    /**
     * 读取并拼接代码目录下的所有代码文件
     */
    private static String readAndConcatenateCodeFiles(String codeDir) {
        if (StrUtil.isBlank(codeDir)) {
            return "";
        }

        File directory = new File(codeDir);
        if (!directory.exists() || !directory.isDirectory()) {
            log.error("代码目录不存在或不是目录: {}", codeDir);
            return "";
        }

        StringBuilder codeContent = new StringBuilder();
        codeContent.append("# 项目文件结构和代码内容\n\n");

        // 使用 Hutool 的 walkFiles 方法遍历所有文件（访问者模式）
        FileUtil.walkFiles(directory, file -> {
            // 过滤条件：跳过隐藏文件、特定目录下的文件、非代码文件
            if (shouldSkipFile(file, directory)) {
                return;
            }

            if (isCodeFile(file)) {
                String relativePath = FileUtil.subPath(directory.getAbsolutePath(), file.getAbsolutePath());
                codeContent.append("## 文件: ").append(relativePath).append("\n\n");
                codeContent.append("```\n");
                String fileContent = FileUtil.readUtf8String(file);
                codeContent.append(fileContent).append("\n");
                codeContent.append("```\n\n");
            }
        });

        return codeContent.toString();
    }

    /**
     * 判断是否应该跳过此文件
     */
    private static boolean shouldSkipFile(File file, File rootDir) {
        String relativePath = FileUtil.subPath(rootDir.getAbsolutePath(), file.getAbsolutePath());

        // 跳过隐藏文件
        if (file.getName().startsWith(".")) {
            return true;
        }

        // 跳过特定目录下的文件
        return relativePath.contains("node_modules" + File.separator) ||
                relativePath.contains("dist" + File.separator) ||
                relativePath.contains("target" + File.separator) ||
                relativePath.contains(".git" + File.separator);
    }

    /**
     * 判断是否是需要检查的代码文件
     */
    private static boolean isCodeFile(File file) {
        String fileName = file.getName().toLowerCase();
        return CODE_EXTENSIONS.stream().anyMatch(fileName::endsWith);
    }
}
