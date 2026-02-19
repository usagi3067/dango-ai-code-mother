package com.dango.dangoaicodeapp.domain.codegen.tools;

import java.util.Set;

/**
 * 基础设施文件保护检查器
 * 防止 AI 通过工具调用覆盖系统预置的模板文件
 */
public final class ProtectedFileChecker {

    private static final Set<String> PROTECTED_FILES = Set.of(
            "index.html",
            "package.json",
            "package-lock.json",
            "vite.config.js",
            "src/main.js"
    );

    private ProtectedFileChecker() {
    }

    public static boolean isProtected(String relativeFilePath) {
        if (relativeFilePath == null || relativeFilePath.isBlank()) {
            return false;
        }
        String normalized = relativeFilePath.replace('\\', '/').trim();
        if (normalized.startsWith("./")) {
            normalized = normalized.substring(2);
        }
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return PROTECTED_FILES.contains(normalized);
    }

    public static String buildErrorMessage(String relativeFilePath) {
        return "错误：" + relativeFilePath + " 是系统预置的基础设施文件，禁止写入或修改。" +
                "请通过修改 src/ 目录下的业务文件（如 App.vue、pages/*.vue、components/*.vue）来实现需求。";
    }
}
