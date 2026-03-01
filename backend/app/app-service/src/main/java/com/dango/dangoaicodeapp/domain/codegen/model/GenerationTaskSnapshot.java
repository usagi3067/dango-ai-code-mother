package com.dango.dangoaicodeapp.domain.codegen.model;

/**
 * 代码生成任务快照。
 *
 * 统一描述任务状态与绑定的聊天消息，避免上层直接依赖 Redis Hash 字段细节。
 */
public record GenerationTaskSnapshot(String status, Long chatHistoryId) {

    public static final String STATUS_NONE = "none";
    public static final String STATUS_GENERATING = "generating";
    public static final String STATUS_COMPLETED = "completed";
    public static final String STATUS_ERROR = "error";

    public GenerationTaskSnapshot {
        status = (status == null || status.isBlank()) ? STATUS_NONE : status;
    }

    public static GenerationTaskSnapshot none() {
        return new GenerationTaskSnapshot(STATUS_NONE, null);
    }

    public boolean isNone() {
        return STATUS_NONE.equals(status);
    }

    public boolean isGenerating() {
        return STATUS_GENERATING.equals(status);
    }

    public boolean isTerminal() {
        return STATUS_COMPLETED.equals(status) || STATUS_ERROR.equals(status);
    }
}
