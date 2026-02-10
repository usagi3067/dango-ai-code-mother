package com.dango.dangoaicodeapp.workflow.node;

import com.dango.dangoaicodeapp.model.enums.CodeGenTypeEnum;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CodeReaderNode 单元测试
 * 测试代码读取节点的核心逻辑
 *
 * @author dango
 */
class CodeReaderNodeTest {

    @TempDir
    Path tempDir;

    private String originalUserDir;

    @BeforeEach
    void setUp() {
        originalUserDir = System.getProperty("user.dir");
        System.setProperty("user.dir", tempDir.toString());
    }

    @AfterEach
    void tearDown() {
        if (originalUserDir != null) {
            System.setProperty("user.dir", originalUserDir);
        }
    }

    @Test
    @DisplayName("读取 HTML 单文件项目结构")
    void testReadHtmlSingleFileProjectStructure() throws IOException {
        Long appId = 100L;
        Path codeOutputDir = tempDir.resolve("tmp/code_output");
        Files.createDirectories(codeOutputDir);
        Path htmlDir = codeOutputDir.resolve("html_" + appId);
        Files.createDirectories(htmlDir);
        Files.writeString(htmlDir.resolve("index.html"), "<!DOCTYPE html>");

        String structure = CodeReaderNode.readProjectStructure(appId, CodeGenTypeEnum.HTML);

        assertNotNull(structure);
        assertTrue(structure.contains("index.html"));
        assertTrue(structure.contains("项目目录结构:"));
    }

    @Test
    @DisplayName("读取多文件项目结构")
    void testReadMultiFileProjectStructure() throws IOException {
        Long appId = 200L;
        Path codeOutputDir = tempDir.resolve("tmp/code_output");
        Files.createDirectories(codeOutputDir);
        Path multiFileDir = codeOutputDir.resolve("multi_file_" + appId);
        Files.createDirectories(multiFileDir);
        
        Files.writeString(multiFileDir.resolve("index.html"), "<!DOCTYPE html>");
        Files.writeString(multiFileDir.resolve("style.css"), "body {}");
        Files.writeString(multiFileDir.resolve("script.js"), "console.log('hello');");

        String structure = CodeReaderNode.readProjectStructure(appId, CodeGenTypeEnum.MULTI_FILE);

        assertNotNull(structure);
        assertTrue(structure.contains("index.html"));
        assertTrue(structure.contains("style.css"));
        assertTrue(structure.contains("script.js"));
    }

    @Test
    @DisplayName("文件不存在时返回 null")
    void testReadProjectStructureWhenNotExists() {
        Long appId = 999L;
        String structure = CodeReaderNode.readProjectStructure(appId, CodeGenTypeEnum.HTML);
        assertNull(structure);
    }

    @Test
    @DisplayName("无效 appId 时返回 null")
    void testReadProjectStructureWithInvalidAppId() {
        assertNull(CodeReaderNode.readProjectStructure(null, CodeGenTypeEnum.HTML));
        assertNull(CodeReaderNode.readProjectStructure(0L, CodeGenTypeEnum.HTML));
        assertNull(CodeReaderNode.readProjectStructure(-1L, CodeGenTypeEnum.HTML));
    }

    @Test
    @DisplayName("未指定代码类型时自动查找项目目录")
    void testReadProjectStructureWithoutCodeGenType() throws IOException {
        Long appId = 300L;
        Path codeOutputDir = tempDir.resolve("tmp/code_output");
        Files.createDirectories(codeOutputDir);
        Path vueDir = codeOutputDir.resolve("vue_project_" + appId);
        Files.createDirectories(vueDir);
        Files.writeString(vueDir.resolve("package.json"), "{}");

        String structure = CodeReaderNode.readProjectStructure(appId, null);

        assertNotNull(structure);
        assertTrue(structure.contains("package.json"));
    }

    @Test
    @DisplayName("shouldIgnore 应正确过滤")
    void testShouldIgnore() {
        // 应该过滤的
        assertTrue(CodeReaderNode.shouldIgnore("node_modules"));
        assertTrue(CodeReaderNode.shouldIgnore(".git"));
        assertTrue(CodeReaderNode.shouldIgnore("app.log"));
        
        // 不应该过滤的
        assertFalse(CodeReaderNode.shouldIgnore("index.html"));
        assertFalse(CodeReaderNode.shouldIgnore("App.vue"));
    }

    @Test
    @DisplayName("读取项目结构时应过滤 node_modules")
    void testReadProjectStructureFiltersNodeModules() throws IOException {
        Long appId = 400L;
        Path codeOutputDir = tempDir.resolve("tmp/code_output");
        Files.createDirectories(codeOutputDir);
        Path vueDir = codeOutputDir.resolve("vue_project_" + appId);
        Files.createDirectories(vueDir);
        
        Files.writeString(vueDir.resolve("package.json"), "{}");
        Path nodeModules = vueDir.resolve("node_modules");
        Files.createDirectories(nodeModules);
        Files.writeString(nodeModules.resolve("some-package.js"), "module.exports = {}");

        String structure = CodeReaderNode.readProjectStructure(appId, CodeGenTypeEnum.VUE_PROJECT);

        assertNotNull(structure);
        assertTrue(structure.contains("package.json"));
        assertFalse(structure.contains("some-package.js"));
    }

    @Test
    @DisplayName("getProjectPath 应返回正确的项目路径")
    void testGetProjectPath() throws IOException {
        Long appId = 500L;
        Path codeOutputDir = tempDir.resolve("tmp/code_output");
        Files.createDirectories(codeOutputDir);
        Path htmlDir = codeOutputDir.resolve("html_" + appId);
        Files.createDirectories(htmlDir);

        Path projectPath = CodeReaderNode.getProjectPath(appId, CodeGenTypeEnum.HTML);

        assertNotNull(projectPath);
        assertTrue(projectPath.toString().contains("html_" + appId));
    }

    @Test
    @DisplayName("getProjectPath 对于不存在的项目应返回 null")
    void testGetProjectPathNotExists() {
        Path projectPath = CodeReaderNode.getProjectPath(999L, CodeGenTypeEnum.HTML);
        assertNull(projectPath);
    }
}
