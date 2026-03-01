package com.dango.dangoaicodeapp.infrastructure.repository;

import com.dango.dangoaicodeapp.domain.codegen.builder.VueProjectBuilder;
import com.dango.dangoaicodeapp.domain.codegen.model.ProjectBuildResult;
import com.dango.dangoaicodeapp.domain.codegen.port.ProjectBuildPort;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 项目构建端口适配器。
 */
@Component
public class ProjectBuildPortImpl implements ProjectBuildPort {

    @Resource
    private VueProjectBuilder vueProjectBuilder;

    @Override
    public ProjectBuildResult buildProject(String generatedCodeDir) {
        VueProjectBuilder.BuildResult result = vueProjectBuilder.buildProjectWithResult(generatedCodeDir);
        return new ProjectBuildResult(result.isSuccess(), result.getErrorSummary(), result.getStderr());
    }
}
