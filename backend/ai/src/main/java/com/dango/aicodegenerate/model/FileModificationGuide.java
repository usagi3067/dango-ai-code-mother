package com.dango.aicodegenerate.model;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 文件修改指导
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileModificationGuide {
    @JsonPropertyDescription("文件相对路径，如 src/pages/Index.vue")
    private String path;

    @JsonPropertyDescription("修改类型：MODIFY（修改现有文件）、CREATE（创建新文件）、DELETE（删除文件）")
    private String type;

    @JsonPropertyDescription("具体操作列表，每项描述一个明确的修改动作")
    private List<String> operations;

    @JsonPropertyDescription("修改原因，说明为什么要修改这个文件")
    private String reason;
}
