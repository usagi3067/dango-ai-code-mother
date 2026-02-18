package com.dango.dangoaicodeuser.interfaces.controller;

import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.apm.toolkit.trace.TraceContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查控制器 - 用于验证 SkyWalking traceId 集成
 */
@Slf4j
@RestController
@RequestMapping("/health")
public class HealthController {

    @GetMapping("/check")
    public Map<String, Object> healthCheck() {
        // 1. 打印普通日志，验证日志格式中的 traceId 占位符
        log.info("=== 健康检查开始 ===");
        log.info("服务名称: user-service");

        // 2. 手动获取 traceId，验证 SkyWalking Agent 是否正常工作
        String traceId = TraceContext.traceId();
        log.info("当前 traceId: {}", traceId);

        // 3. 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("service", "user-service");
        result.put("status", "UP");
        result.put("traceId", traceId);
        result.put("traceIdValid", traceId != null && !traceId.isEmpty() && !"N/A".equals(traceId));

        log.info("=== 健康检查结束 ===");
        return result;
    }
}
