package com.dango.dangoaicodeapp.domain.codegen.port;

import com.dango.dangoaicodeapp.domain.codegen.model.ProjectBuildResult;

/**
 * 项目构建端口。
 */
public interface ProjectBuildPort {

    ProjectBuildResult buildProject(String generatedCodeDir);
}
