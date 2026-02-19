package com.dango.dangoaicodeapp.domain.codegen.scaffold;

import com.dango.dangoaicodeapp.domain.app.valueobject.CodeGenTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class LeetCodeScaffoldService implements ProjectScaffoldService {

    private static final String TEMPLATE_BASE_PATH = "templates/leetcode-project/";

    private static final List<String> TEMPLATE_FILES = List.of(
            "index.html",
            "package.json",
            "vite.config.ts",
            "tsconfig.json",
            "env.d.ts",
            "src/main.ts",
            "src/App.vue",
            "src/components/AnimationControls.vue",
            "src/components/AnimationDemo.vue",
            "src/components/CodePanel.vue",
            "src/components/CompareTable.vue",
            "src/components/CoreIdea.vue",
            "src/components/ExplanationBox.vue",
            "src/components/TabContainer.vue",
            "src/components/visualizations/PlaceholderVis.vue",
            "src/composables/useAnimation.ts",
            "src/data/problem.ts",
            "src/data/solutions.ts",
            "src/styles/theme.css",
            "src/types/index.ts"
    );

    private final NodeModulesPrebuilder nodeModulesPrebuilder;

    @Override
    public void scaffold(Long appId) {
        Path targetDir = buildTargetDir(appId);

        if (Files.exists(targetDir)) {
            log.info("项目目录已存在，跳过脚手架复制: {}", targetDir);
            return;
        }

        log.info("开始复制 LeetCode 项目模板到: {}", targetDir);
        copyTemplateFiles(targetDir);
        linkSharedNodeModules(targetDir);
        log.info("LeetCode 项目模板复制完成: {}", targetDir);
    }

    @Override
    public CodeGenTypeEnum getType() {
        return CodeGenTypeEnum.LEETCODE_PROJECT;
    }

    private Path buildTargetDir(Long appId) {
        String baseDir = System.getProperty("user.dir") + File.separator + "tmp" + File.separator + "code_output";
        String dirName = "leetcode_project_" + appId;
        return Path.of(baseDir, dirName);
    }

    private void copyTemplateFiles(Path targetDir) {
        for (String relativePath : TEMPLATE_FILES) {
            ClassPathResource resource = new ClassPathResource(TEMPLATE_BASE_PATH + relativePath);
            Path targetFile = targetDir.resolve(relativePath);

            try {
                Files.createDirectories(targetFile.getParent());
                try (InputStream inputStream = resource.getInputStream()) {
                    Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
                }
                log.debug("复制模板文件: {}", relativePath);
            } catch (IOException e) {
                throw new RuntimeException("复制模板文件失败: " + relativePath, e);
            }
        }
    }

    /**
     * 创建 node_modules 符号链接指向预构建目录
     */
    private void linkSharedNodeModules(Path targetDir) {
        if (!nodeModulesPrebuilder.isReady()) {
            log.warn("预构建 node_modules 未就绪，跳过 symlink 创建");
            return;
        }

        Path link = targetDir.resolve("node_modules");
        Path target = nodeModulesPrebuilder.getPrebuiltNodeModulesPath();

        try {
            Files.createSymbolicLink(link, target);
            log.info("创建 node_modules symlink: {} -> {}", link, target);
        } catch (IOException e) {
            log.warn("创建 node_modules symlink 失败，将回退到 npm install: {}", e.getMessage());
        }
    }
}
