package com.dango.dangoaicodeapp.infrastructure.scaffold;

import com.dango.dangoaicodeapp.domain.app.valueobject.CodeGenTypeEnum;

public interface ProjectScaffoldService {
    void scaffold(Long appId);
    CodeGenTypeEnum getType();
}
