package com.dango.dangoaicodeapp.domain.codegen.model;

/**
 * 项目构建结果。
 */
public record ProjectBuildResult(boolean success, String errorSummary, String stderr) {
}
