package com.dango.dangoaicodeapp.domain.codegen.scaffold;

import cn.hutool.core.util.RuntimeUtil;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;

/**
 * node_modules 预构建服务
 * 服务启动时预先执行 npm install，生成共享的 node_modules 目录
 */
@Slf4j
@Component
public class NodeModulesPrebuilder {

    private static final String PREBUILT_DIR_NAME = "_shared_node_modules";
    private static final String TEMPLATE_PACKAGE_JSON = "templates/vue-project/package.json";

    private Path prebuiltDir;
    private volatile boolean ready = false;

    @PostConstruct
    public void init() {
        String baseDir = System.getProperty("user.dir") + File.separator + "tmp" + File.separator + "code_output";
        this.prebuiltDir = Path.of(baseDir, PREBUILT_DIR_NAME);
        Thread.ofVirtual().name("node-modules-prebuilder").start(this::prebuild);
    }

    /**
     * 获取预构建的 node_modules 绝对路径
     */
    public Path getPrebuiltNodeModulesPath() {
        return prebuiltDir.resolve("node_modules");
    }

    /**
     * 预构建是否就绪
     */
    public boolean isReady() {
        return ready;
    }

    /**
     * 强制重建（依赖变更时调用）
     */
    public synchronized void rebuild() {
        ready = false;
        try {
            // 删除旧的 node_modules
            Path nodeModules = getPrebuiltNodeModulesPath();
            if (Files.exists(nodeModules)) {
                deleteDirectory(nodeModules);
            }
            prebuild();
        } catch (IOException e) {
            log.error("重建预构建 node_modules 失败", e);
        }
    }

    private void prebuild() {
        try {
            Path nodeModules = getPrebuiltNodeModulesPath();
            if (Files.exists(nodeModules) && Files.isDirectory(nodeModules)) {
                log.info("预构建 node_modules 已存在，跳过: {}", nodeModules);
                ready = true;
                return;
            }

            Files.createDirectories(prebuiltDir);

            // 复制 package.json 到预构建目录
            ClassPathResource resource = new ClassPathResource(TEMPLATE_PACKAGE_JSON);
            Path targetPackageJson = prebuiltDir.resolve("package.json");
            try (InputStream is = resource.getInputStream()) {
                Files.copy(is, targetPackageJson, StandardCopyOption.REPLACE_EXISTING);
            }

            // 执行 npm install
            log.info("开始预构建 node_modules: {}", prebuiltDir);
            Process process = RuntimeUtil.exec(null, prebuiltDir.toFile(), "npm", "install");

            boolean finished = process.waitFor(300, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.error("预构建 npm install 超时");
                return;
            }

            if (process.exitValue() != 0) {
                String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
                log.error("预构建 npm install 失败: {}", stderr.length() > 500 ? stderr.substring(stderr.length() - 500) : stderr);
                return;
            }

            ready = true;
            log.info("预构建 node_modules 完成: {}", nodeModules);
        } catch (Exception e) {
            log.error("预构建 node_modules 异常", e);
        }
    }

    private void deleteDirectory(Path dir) throws IOException {
        try (var walk = Files.walk(dir)) {
            walk.sorted(java.util.Comparator.reverseOrder())
                .forEach(path -> {
                    try { Files.delete(path); } catch (IOException e) { /* ignore */ }
                });
        }
    }
}
