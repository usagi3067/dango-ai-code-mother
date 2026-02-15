package com.dango.dangoaicodeapp.core.scaffold;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * HTML 到 Vue 项目转换服务
 * <p>
 * 用户上传 HTML 文件后，自动创建 Vue 项目脚手架并将 HTML 内容
 * 保存为 src/legacy.html，供 AI 在首次对话时参考并转换为 Vue 组件。
 */
@Slf4j
@Component
public class HtmlToVueConverterService {

    @Resource
    private VueProjectScaffoldService scaffoldService;

    /**
     * 将上传的 HTML 内容转换为 Vue 项目结构
     * <p>
     * 1. 调用脚手架服务创建 Vue 项目模板
     * 2. 将 HTML 内容保存为 src/legacy.html
     *
     * @param appId       应用 ID
     * @param htmlContent HTML 文件内容
     */
    public void convert(Long appId, String htmlContent) {
        // 1. 创建 Vue 项目脚手架
        scaffoldService.scaffold(appId);

        // 2. 将 HTML 内容保存为 src/legacy.html
        Path projectDir = buildProjectDir(appId);
        Path legacyHtml = projectDir.resolve("src").resolve("legacy.html");

        try {
            Files.createDirectories(legacyHtml.getParent());
            Files.writeString(legacyHtml, htmlContent, StandardCharsets.UTF_8);
            log.info("HTML 内容已保存到 {}", legacyHtml);
        } catch (IOException e) {
            throw new RuntimeException("保存 legacy.html 失败: " + e.getMessage(), e);
        }
    }

    private Path buildProjectDir(Long appId) {
        String baseDir = System.getProperty("user.dir") + File.separator + "tmp" + File.separator + "code_output";
        return Path.of(baseDir, "vue_project_" + appId);
    }
}
