package com.dango.dangoaicodeapp.domain.codegen.port;

import com.dango.aicodegenerate.model.ModificationPlanResult;

/**
 * 修改规划能力领域端口。
 */
public interface ModificationPlanningGateway {

    ModificationPlanResult plan(long appId, String planningRequest);
}
