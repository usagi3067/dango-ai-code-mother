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

    private static final String LEETCODE_PREBUILT_DIR_NAME = "_shared_leetcode_node_modules";
    private static final String LEETCODE_TEMPLATE_PACKAGE_JSON = "templates/leetcode-project/package.json";

    private Path prebuiltDir;
    private volatile boolean ready = false;

    private Path leetCodePrebuiltDir;
    private volatile boolean leetCodeReady = false;

    @PostConstruct
    public void init() {
        String baseDir = System.getProperty("user.dir") + File.separator + "tmp" + File.separator + "code_output";
        this.prebuiltDir = Path.of(baseDir, PREBUILT_DIR_NAME);
        this.leetCodePrebuiltDir = Path.of(baseDir, LEETCODE_PREBUILT_DIR_NAME);
        Thread.ofVirtual().name("node-modules-prebuilder-vue").start(() -> prebuild(prebuiltDir, TEMPLATE_PACKAGE_JSON, "vue"));
        Thread.ofVirtual().name("node-modules-prebuilder-leetcode").start(() -> prebuild(leetCodePrebuiltDir, LEETCODE_TEMPLATE_PACKAGE_JSON, "leetcode"));
    }

    /**
     * 获取预构建的 vue node_modules 绝对路径
     */
    public Path getPrebuiltNodeModulesPath() {
        return prebuiltDir.resolve("node_modules");
    }

    /**
     * vue 预构建是否就绪
     */
    public boolean isReady() {
        return ready;
    }

    /**
     * 获取预构建的 leetcode node_modules 绝对路径
     */
    public Path getLeetCodePrebuiltNodeModulesPath() {
        return leetCodePrebuiltDir.resolve("node_modules");
    }

    /**
     * leetcode 预构建是否就绪
     */
    public boolean isLeetCodeReady() {
        return leetCodeReady;
    }

    /**
     * 强制重建（依赖变更时调用）
     */
    public synchronized void rebuild() {
        ready = false;
        leetCodeReady = false;
        try {
            Path vueNodeModules = getPrebuiltNodeModulesPath();
            if (Files.exists(vueNodeModules)) {
                deleteDirectory(vueNodeModules);
            }
            Path leetCodeNodeModules = getLeetCodePrebuiltNodeModulesPath();
            if (Files.exists(leetCodeNodeModules)) {
                deleteDirectory(leetCodeNodeModules);
            }
        } catch (IOException e) {
            log.error("删除预构建 node_modules 失败", e);
        }
        Thread.ofVirtual().name("node-modules-prebuilder-vue").start(() -> prebuild(prebuiltDir, TEMPLATE_PACKAGE_JSON, "vue"));
        Thread.ofVirtual().name("node-modules-prebuilder-leetcode").start(() -> prebuild(leetCodePrebuiltDir, LEETCODE_TEMPLATE_PACKAGE_JSON, "leetcode"));
    }

    private void prebuild(Path targetDir, String packageJsonResource, String label) {
        try {
            Path nodeModules = targetDir.resolve("node_modules");
            if (Files.exists(nodeModules) && Files.isDirectory(nodeModules)) {
                log.info("[{}] 预构建 node_modules 已存在，跳过: {}", label, nodeModules);
                setReady(label, true);
                return;
            }

            Files.createDirectories(targetDir);

            // 复制 package.json 到预构建目录
            ClassPathResource resource = new ClassPathResource(packageJsonResource);
            Path targetPackageJson = targetDir.resolve("package.json");
            try (InputStream is = resource.getInputStream()) {
                Files.copy(is, targetPackageJson, StandardCopyOption.REPLACE_EXISTING);
            }

            // 执行 npm install
            log.info("[{}] 开始预构建 node_modules: {}", label, targetDir);
            Process process = RuntimeUtil.exec(null, targetDir.toFile(), "npm", "install");

            boolean finished = process.waitFor(300, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.error("[{}] 预构建 npm install 超时", label);
                return;
            }

            if (process.exitValue() != 0) {
                String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
                log.error("[{}] 预构建 npm install 失败: {}", label, stderr.length() > 500 ? stderr.substring(stderr.length() - 500) : stderr);
                return;
            }

            setReady(label, true);
            log.info("[{}] 预构建 node_modules 完成: {}", label, nodeModules);
        } catch (Exception e) {
            log.error("[{}] 预构建 node_modules 异常", label, e);
        }
    }

    private void setReady(String label, boolean value) {
        if ("vue".equals(label)) {
            this.ready = value;
        } else if ("leetcode".equals(label)) {
            this.leetCodeReady = value;
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
