package com.dango.dangoaicodeapp.infrastructure.repository;

import com.dango.dangoaicodeapp.domain.app.valueobject.CodeGenTypeEnum;
import com.dango.dangoaicodeapp.domain.codegen.port.ProjectScaffoldPort;
import com.dango.dangoaicodeapp.domain.codegen.scaffold.ProjectScaffoldServiceFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 项目脚手架端口适配器。
 */
@Component
@RequiredArgsConstructor
public class ProjectScaffoldPortImpl implements ProjectScaffoldPort {

    private final ProjectScaffoldServiceFactory scaffoldServiceFactory;

    @Override
    public void scaffold(Long appId, CodeGenTypeEnum generationType) {
        scaffoldServiceFactory.getService(generationType).scaffold(appId);
    }
}

