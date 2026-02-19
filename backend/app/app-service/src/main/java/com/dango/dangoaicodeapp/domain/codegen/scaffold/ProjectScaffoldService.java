package com.dango.dangoaicodeapp.domain.codegen.scaffold;

import com.dango.dangoaicodeapp.domain.app.valueobject.CodeGenTypeEnum;

public interface ProjectScaffoldService {
    void scaffold(Long appId);
    CodeGenTypeEnum getType();
}
