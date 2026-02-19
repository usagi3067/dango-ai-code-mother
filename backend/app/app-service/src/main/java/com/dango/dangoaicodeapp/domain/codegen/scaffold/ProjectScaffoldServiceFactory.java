package com.dango.dangoaicodeapp.domain.codegen.scaffold;

import com.dango.dangoaicodeapp.domain.app.valueobject.CodeGenTypeEnum;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ProjectScaffoldServiceFactory {
    private final Map<CodeGenTypeEnum, ProjectScaffoldService> services;

    public ProjectScaffoldServiceFactory(List<ProjectScaffoldService> serviceList) {
        this.services = serviceList.stream()
                .collect(Collectors.toMap(ProjectScaffoldService::getType, Function.identity()));
    }

    public ProjectScaffoldService getService(CodeGenTypeEnum type) {
        ProjectScaffoldService service = services.get(type);
        if (service == null) {
            throw new IllegalArgumentException("不支持的脚手架类型: " + type);
        }
        return service;
    }
}
