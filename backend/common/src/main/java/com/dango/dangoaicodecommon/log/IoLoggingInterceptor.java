package com.dango.dangoaicodecommon.log;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.nio.charset.StandardCharsets;

/**
 * HTTP 请求/响应 IO 日志拦截器
 * 记录 Controller 层的请求参数和响应信息
 */
@Component
public class IoLoggingInterceptor implements HandlerInterceptor {

    private static final Logger IO_LOG = LoggerFactory.getLogger("IO_LOG");
    private static final int MAX_BODY_LENGTH = 2048;
    private static final String START_TIME_ATTR = "io_log_start_time";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME_ATTR, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        long startTime = (long) request.getAttribute(START_TIME_ATTR);
        long duration = System.currentTimeMillis() - startTime;

        String method = request.getMethod();
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();
        int status = response.getStatus();

        // 请求体
        String requestBody = extractBody(request);

        // 请求头（仅记录 Content-Type）
        String contentType = request.getContentType();

        StringBuilder sb = new StringBuilder();
        sb.append("[HTTP] ").append(method).append(" ").append(uri);
        if (queryString != null) {
            sb.append("?").append(queryString);
        }
        sb.append(" | status=").append(status);
        sb.append(" | ").append(duration).append("ms");
        if (contentType != null) {
            sb.append(" | contentType=").append(contentType);
        }
        if (!requestBody.isEmpty()) {
            sb.append(" | body=").append(requestBody);
        }
        if (ex != null) {
            sb.append(" | exception=").append(ex.getMessage());
        }

        if (status >= 400 || ex != null) {
            IO_LOG.warn(sb.toString());
        } else {
            IO_LOG.info(sb.toString());
        }
    }

    private String extractBody(HttpServletRequest request) {
        if (request instanceof ContentCachingRequestWrapper wrapper) {
            byte[] buf = wrapper.getContentAsByteArray();
            if (buf.length == 0) {
                return "";
            }
            String text = new String(buf, StandardCharsets.UTF_8);
            return text.length() > MAX_BODY_LENGTH
                    ? text.substring(0, MAX_BODY_LENGTH) + "...(truncated)"
                    : text;
        }
        return "";
    }
}
