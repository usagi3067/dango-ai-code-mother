package com.dango.dangoaicodeapp.domain.codegen.port;

import com.dango.aicodegenerate.model.QualityResult;

/**
 * 代码质量检查领域端口。
 */
public interface CodeQualityCheckGateway {

    QualityResult checkCodeQuality(String codeContent);
}
