package com.dango.dangoaicodeapp.infrastructure.repository;

import cn.hutool.core.io.FileUtil;
import com.dango.dangoaicodeapp.domain.app.valueobject.CodeGenTypeEnum;
import com.dango.dangoaicodeapp.domain.codegen.port.ProjectWorkspacePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

/**
 * 项目工作区端口适配器。
 */
@Slf4j
@Component
public class ProjectWorkspacePortImpl implements ProjectWorkspacePort {

    private static final String CODE_OUTPUT_ROOT_DIR = System.getProperty("user.dir") + "/tmp/code_output";

    private static final Set<String> IGNORED_NAMES = Set.of(
            "node_modules", ".git", "dist", "build", ".DS_Store",
            ".env", "target", ".mvn", ".idea", ".vscode", "coverage"
    );

    private static final Set<String> IGNORED_EXTENSIONS = Set.of(
            ".log", ".tmp", ".cache", ".lock"
    );

    @Override
    public boolean hasExistingCode(Long appId, CodeGenTypeEnum generationType) {
        if (appId == null || appId <= 0) {
            return false;
        }
        Path codePath = resolveProjectPath(appId, generationType);
        return codePath != null && Files.exists(codePath);
    }

    @Override
    public String readProjectStructure(Long appId, CodeGenTypeEnum generationType) {
        if (appId == null || appId <= 0) {
            log.warn("无效的 appId: {}", appId);
            return null;
        }

        Path projectPath = resolveProjectPath(appId, generationType);
        if (projectPath == null) {
            log.info("未找到 appId={} 的项目目录", appId);
            return null;
        }

        File targetDir = projectPath.toFile();
        if (!targetDir.exists() || !targetDir.isDirectory()) {
            return null;
        }

        StringBuilder structure = new StringBuilder();
        structure.append("项目目录结构:\n");

        List<File> allFiles = FileUtil.loopFiles(targetDir, file -> {
            if (shouldIgnore(file.getName())) {
                return false;
            }
            File parent = file.getParentFile();
            while (parent != null && !parent.equals(targetDir)) {
                if (shouldIgnore(parent.getName())) {
                    return false;
                }
                parent = parent.getParentFile();
            }
            return true;
        });

        allFiles.stream()
                .sorted((f1, f2) -> f1.getPath().compareTo(f2.getPath()))
                .forEach(file -> {
                    String relativePath = targetDir.toPath().relativize(file.toPath()).toString();
                    structure.append("- ").append(relativePath).append("\n");
                });

        return structure.toString();
    }

    @Override
    public CodeGenTypeEnum inferGenerationType(Long appId) {
        if (appId == null || appId <= 0) {
            return CodeGenTypeEnum.VUE_PROJECT;
        }
        for (CodeGenTypeEnum type : CodeGenTypeEnum.values()) {
            Path path = Path.of(CODE_OUTPUT_ROOT_DIR, type.getValue() + "_" + appId);
            if (Files.exists(path)) {
                return type;
            }
        }
        return CodeGenTypeEnum.VUE_PROJECT;
    }

    @Override
    public String buildGeneratedCodeDir(CodeGenTypeEnum generationType, Long appId) {
        CodeGenTypeEnum effectiveType = generationType == null ? CodeGenTypeEnum.VUE_PROJECT : generationType;
        return CODE_OUTPUT_ROOT_DIR + File.separator + effectiveType.getValue() + "_" + appId;
    }

    private Path resolveProjectPath(Long appId, CodeGenTypeEnum generationType) {
        String dirName = (generationType != null ? generationType.getValue() : "vue_project") + "_" + appId;
        Path projectPath = Paths.get(CODE_OUTPUT_ROOT_DIR, dirName);
        if (Files.exists(projectPath)) {
            return projectPath;
        }
        return null;
    }

    private boolean shouldIgnore(String fileName) {
        if (IGNORED_NAMES.contains(fileName)) {
            return true;
        }
        return IGNORED_EXTENSIONS.stream().anyMatch(fileName::endsWith);
    }
}
