package com.dango.dangoaicodeapp.domain.codegen.port;

import com.dango.dangoaicodeapp.domain.app.valueobject.CodeGenTypeEnum;

/**
 * 项目脚手架端口。
 *
 * <p>为何抽象：CodeGeneratorNode 只表达“生成前确保模板就绪”的业务意图，
 * 不关心模板文件如何复制、如何创建软链等基础设施细节。
 */
public interface ProjectScaffoldPort {

    /**
     * 为指定应用和生成类型准备脚手架。
     */
    void scaffold(Long appId, CodeGenTypeEnum generationType);
}

