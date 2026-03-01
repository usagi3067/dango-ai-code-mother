package com.dango.dangoaicodeapp.domain.codegen.port;

import com.dango.dangoaicodeapp.domain.app.valueobject.CodeGenTypeEnum;

/**
 * 项目工作区端口。
 * 封装代码目录探测、结构读取与路径规则，避免节点直接依赖文件系统。
 */
public interface ProjectWorkspacePort {

    boolean hasExistingCode(Long appId, CodeGenTypeEnum generationType);

    String readProjectStructure(Long appId, CodeGenTypeEnum generationType);

    CodeGenTypeEnum inferGenerationType(Long appId);

    String buildGeneratedCodeDir(CodeGenTypeEnum generationType, Long appId);
}
