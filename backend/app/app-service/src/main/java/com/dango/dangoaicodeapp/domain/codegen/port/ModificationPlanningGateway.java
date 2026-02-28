package com.dango.dangoaicodeapp.domain.codegen.port;

import com.dango.aicodegenerate.model.ModificationPlanResult;
import dev.langchain4j.service.TokenStream;

/**
 * 修改规划能力领域端口。
 */
public interface ModificationPlanningGateway {

    ModificationPlanResult plan(long appId, String planningRequest);

    TokenStream planStream(long appId, String planningRequest);
}
