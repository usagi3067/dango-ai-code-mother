package com.dango.dangoaicodeapp.domain.codegen.port;

import com.dango.aicodegenerate.model.AppNameAndTagResult;

/**
 * 应用信息生成能力领域端口。
 */
public interface AppInfoGenerationPort {

    AppNameAndTagResult generateAppInfo(String userDescription);
}
