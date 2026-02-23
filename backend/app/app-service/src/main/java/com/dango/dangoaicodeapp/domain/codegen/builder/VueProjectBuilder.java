package com.dango.dangoaicodeapp.domain.codegen.builder;

import cn.hutool.core.util.RuntimeUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * @author dango
 * @description
 * @date
 */
@Slf4j
@Component
public class VueProjectBuilder {

    private static final int ERROR_SUMMARY_MAX_LENGTH = 2000;

    /**
     * 构建结果，包含 stdout/stderr 和错误摘要
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BuildResult {
        private boolean success;
        private String stdout;
        private String stderr;
        private String errorSummary;

        public static BuildResult success() {
            return new BuildResult(true, "", "", "");
        }

        public static BuildResult failure(String stderr, String errorSummary) {
            return new BuildResult(false, "", stderr, errorSummary);
        }
    }

    /**
     * 执行命令并捕获 stdout/stderr
     *
     * @param workingDir     工作目录
     * @param command        命令字符串
     * @param timeoutSeconds 超时时间（秒）
     * @return 构建结果
     */
    private BuildResult executeCommand(File workingDir, String command, int timeoutSeconds) {
        try {
            log.info("在目录 {} 中执行命令: {}", workingDir.getAbsolutePath(), command);
            Process process = RuntimeUtil.exec(
                    null,
                    workingDir,
                    command.split("\\s+")
            );

            // 用单独的线程读取 stdout 和 stderr，避免缓冲区满导致死锁
            StringBuilder stdoutBuilder = new StringBuilder();
            StringBuilder stderrBuilder = new StringBuilder();

            Thread stdoutThread = Thread.ofVirtual().start(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stdoutBuilder.append(line).append("\n");
                    }
                } catch (Exception e) {
                    log.warn("读取 stdout 异常: {}", e.getMessage());
                }
            });

            Thread stderrThread = Thread.ofVirtual().start(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stderrBuilder.append(line).append("\n");
                    }
                } catch (Exception e) {
                    log.warn("读取 stderr 异常: {}", e.getMessage());
                }
            });

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                log.error("命令执行超时（{}秒），强制终止进程", timeoutSeconds);
                process.destroyForcibly();
                return BuildResult.failure("命令执行超时（" + timeoutSeconds + "秒）",
                        "命令执行超时: " + command);
            }

            // 等待流读取线程完成
            stdoutThread.join(5000);
            stderrThread.join(5000);

            String stdout = stdoutBuilder.toString();
            String stderr = stderrBuilder.toString();
            int exitCode = process.exitValue();

            if (exitCode == 0) {
                log.info("命令执行成功: {}", command);
                BuildResult result = BuildResult.success();
                result.setStdout(stdout);
                result.setStderr(stderr);
                return result;
            } else {
                log.error("命令执行失败，退出码: {}，stderr: {}", exitCode,
                        stderr.length() > 500 ? stderr.substring(stderr.length() - 500) : stderr);
                String errorSummary = extractErrorSummary(stderr);
                return BuildResult.failure(stderr, errorSummary);
            }
        } catch (Exception e) {
            log.error("执行命令失败: {}, 错误信息: {}", command, e.getMessage());
            return BuildResult.failure(e.getMessage(), "命令执行异常: " + e.getMessage());
        }
    }

    /**
     * 从 stderr 中提取错误摘要（取最后 2000 个字符，Vite 错误信息通常在末尾）
     */
    private String extractErrorSummary(String stderr) {
        if (stderr == null || stderr.isBlank()) {
            return "未知错误";
        }
        if (stderr.length() <= ERROR_SUMMARY_MAX_LENGTH) {
            return stderr.trim();
        }
        return stderr.substring(stderr.length() - ERROR_SUMMARY_MAX_LENGTH).trim();
    }

    /**
     * 执行 npm install 命令
     */
    private BuildResult executeNpmInstall(File projectDir) {
        log.info("执行 npm install...");
        return executeCommand(projectDir, "npm install", 300); // 5分钟超时
    }

    /**
     * 执行 npm run build 命令
     */
    private BuildResult executeNpmBuild(File projectDir) {
        log.info("执行 npm run build...");
        return executeCommand(projectDir, "nice -n 10 npm run build", 180); // 3分钟超时，nice降低CPU优先级
    }

    /**
     * 构建 Vue 项目并返回详细结果
     *
     * @param projectPath 项目根目录路径
     * @return 构建结果（包含 stdout/stderr/errorSummary）
     */
    public BuildResult buildProjectWithResult(String projectPath) {
        File projectDir = new File(projectPath);
        if (!projectDir.exists() || !projectDir.isDirectory()) {
            log.error("项目目录不存在: {}", projectPath);
            return BuildResult.failure("", "项目目录不存在: " + projectPath);
        }
        // 检查 package.json 是否存在
        File packageJson = new File(projectDir, "package.json");
        if (!packageJson.exists()) {
            log.error("package.json 文件不存在: {}", packageJson.getAbsolutePath());
            return BuildResult.failure("", "package.json 文件不存在");
        }
        log.info("开始构建 Vue 项目: {}", projectPath);

        // 检测 node_modules 是否已存在（可能通过 symlink 预构建）
        File nodeModules = new File(projectDir, "node_modules");
        if (nodeModules.exists()) {
            log.info("node_modules 已存在，跳过 npm install");
        } else {
            // 回退：执行 npm install
            BuildResult installResult = executeNpmInstall(projectDir);
            if (!installResult.isSuccess()) {
                log.error("npm install 执行失败");
                return BuildResult.failure(installResult.getStderr(),
                        "npm install 失败: " + installResult.getErrorSummary());
            }
        }

        // 执行 npm run build
        BuildResult buildResult = executeNpmBuild(projectDir);
        if (!buildResult.isSuccess()) {
            log.error("npm run build 执行失败");
            return BuildResult.failure(buildResult.getStderr(),
                    "npm run build 失败: " + buildResult.getErrorSummary());
        }

        // 验证 dist 目录是否生成
        File distDir = new File(projectDir, "dist");
        if (!distDir.exists()) {
            log.error("构建完成但 dist 目录未生成: {}", distDir.getAbsolutePath());
            return BuildResult.failure("", "构建完成但 dist 目录未生成");
        }

        log.info("Vue 项目构建成功，dist 目录: {}", distDir.getAbsolutePath());
        return BuildResult.success();
    }

    /**
     * 构建 Vue 项目
     *
     * @param projectPath 项目根目录路径
     * @return 是否构建成功
     */
    public boolean buildProject(String projectPath) {
        return buildProjectWithResult(projectPath).isSuccess();
    }

    /**
     * 异步构建项目（不阻塞主流程）
     *
     * @param projectPath 项目路径
     */
    public void buildProjectAsync(String projectPath) {
        // 在单独的线程中执行构建，避免阻塞主流程
        Thread.ofVirtual().name("vue-builder-" + System.currentTimeMillis()).start(() -> {
            try {
                buildProject(projectPath);
            } catch (Exception e) {
                log.error("异步构建 Vue 项目时发生异常: {}", e.getMessage(), e);
            }
        });
    }

}
