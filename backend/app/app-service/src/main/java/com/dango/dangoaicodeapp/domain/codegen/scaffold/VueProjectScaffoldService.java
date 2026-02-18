package com.dango.dangoaicodeapp.domain.codegen.scaffold;

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

/**
 * Vue 项目模板脚手架服务
 * <p>
 * 从 classpath 中的模板文件复制到项目目录，为 AI 代码生成提供稳定的项目骨架。
 * AI 只需生成业务文件（App.vue、router、pages、components、styles），
 * 不再负责生成 package.json、vite.config.js 等基础设施文件。
 */
@Slf4j
@Component
public class VueProjectScaffoldService {

    private static final String TEMPLATE_BASE_PATH = "templates/vue-project/";

    /**
     * 硬编码模板文件列表（模板文件是固定的，避免 jar 包中 classpath 扫描问题）
     */
    private static final List<String> TEMPLATE_FILES = List.of(
            "index.html",
            "package.json",
            "vite.config.js",
            "src/main.js"
    );

    /**
     * 复制模板到项目目录。
     * 如果目标目录已存在（修改模式），跳过复制。
     *
     * @param appId 应用 ID
     */
    public void scaffold(Long appId) {
        Path targetDir = buildTargetDir(appId);

        if (Files.exists(targetDir)) {
            log.info("项目目录已存在，跳过脚手架复制: {}", targetDir);
            return;
        }

        log.info("开始复制 Vue 项目模板到: {}", targetDir);
        copyTemplateFiles(targetDir);
        log.info("Vue 项目模板复制完成: {}", targetDir);
    }

    /**
     * 构建目标目录路径，与 CodeGeneratorNode.buildGeneratedCodeDir 保持一致
     */
    private Path buildTargetDir(Long appId) {
        String baseDir = System.getProperty("user.dir") + File.separator + "tmp" + File.separator + "code_output";
        String dirName = "vue_project_" + appId;
        return Path.of(baseDir, dirName);
    }

    /**
     * 从 classpath 递归复制所有模板文件到目标目录
     */
    private void copyTemplateFiles(Path targetDir) {
        for (String relativePath : TEMPLATE_FILES) {
            ClassPathResource resource = new ClassPathResource(TEMPLATE_BASE_PATH + relativePath);
            Path targetFile = targetDir.resolve(relativePath);

            try {
                // 确保父目录存在
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
}
