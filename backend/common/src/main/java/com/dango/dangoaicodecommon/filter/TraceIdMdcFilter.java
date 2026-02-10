package com.dango.dangoaicodecommon.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.skywalking.apm.toolkit.trace.TraceContext;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 将 SkyWalking traceId 手动注入 MDC
 * 解决 SkyWalking Agent 字节码增强与 Spring Boot 3.x / logback 1.5.x 的兼容性问题
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdMdcFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_KEY = "tid";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String traceId = TraceContext.traceId();
            if (traceId != null && !traceId.isEmpty() && !"N/A".equalsIgnoreCase(traceId)) {
                MDC.put(TRACE_ID_KEY, traceId);
            }
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TRACE_ID_KEY);
        }
    }
}
