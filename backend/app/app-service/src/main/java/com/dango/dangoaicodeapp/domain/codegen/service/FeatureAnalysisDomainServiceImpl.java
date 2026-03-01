package com.dango.dangoaicodeapp.domain.codegen.service;

import cn.hutool.core.util.StrUtil;
import com.dango.dangoaicodeapp.domain.codegen.model.FeatureAnalysis;
import com.dango.dangoaicodeapp.domain.codegen.port.FeatureAnalysisPort;
import com.dango.dangoaicodecommon.exception.BusinessException;
import com.dango.dangoaicodecommon.exception.ErrorCode;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.net.http.HttpTimeoutException;
import java.util.Locale;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
public class FeatureAnalysisDomainServiceImpl implements FeatureAnalysisDomainService {

    private static final String RETRYABLE_FAILURE_MESSAGE = "AI 服务响应超时或繁忙，请稍后重试";
    private static final String NON_RETRYABLE_FAILURE_MESSAGE = "应用描述无法解析，请补充更具体的功能说明后重试";
    private static final String GENERIC_FAILURE_MESSAGE = "功能分析失败，请稍后重试";

    @Resource
    private FeatureAnalysisPort featureAnalysisPort;

    @Override
    public FeatureAnalysis analyzeFeatures(String prompt, String supplement) {
        String fullPrompt = prompt;
        if (StrUtil.isNotBlank(supplement)) {
            fullPrompt = prompt + "\n\n补充说明：" + supplement;
        }

        try {
            return featureAnalysisPort.analyzeFeatures(fullPrompt);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            if (isRetryableException(e)) {
                log.warn("功能分析失败（可重试）: {}", e.getMessage(), e);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, RETRYABLE_FAILURE_MESSAGE);
            }
            if (isNonRetryableException(e)) {
                log.warn("功能分析失败（不可重试）: {}", e.getMessage(), e);
                throw new BusinessException(ErrorCode.PARAMS_ERROR, NON_RETRYABLE_FAILURE_MESSAGE);
            }
            log.error("功能分析失败（未知异常）", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, GENERIC_FAILURE_MESSAGE);
        }
    }

    private boolean isRetryableException(Throwable throwable) {
        return hasCause(throwable, TimeoutException.class)
                || hasCause(throwable, SocketTimeoutException.class)
                || hasCause(throwable, HttpTimeoutException.class)
                || hasCause(throwable, ConnectException.class)
                || hasCause(throwable, UnknownHostException.class)
                || hasCause(throwable, InterruptedIOException.class)
                || containsAnyKeyword(throwable,
                "timeout", "timed out", "temporarily unavailable", "rate limit", "429");
    }

    private boolean isNonRetryableException(Throwable throwable) {
        return hasCause(throwable, IllegalArgumentException.class)
                || containsAnyKeyword(throwable,
                "bad request", "invalid", "schema", "guardrail", "content policy", "parse");
    }

    private boolean hasCause(Throwable throwable, Class<? extends Throwable> targetType) {
        Throwable current = throwable;
        while (current != null) {
            if (targetType.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean containsAnyKeyword(Throwable throwable, String... keywords) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null) {
                String lowerCaseMessage = message.toLowerCase(Locale.ROOT);
                for (String keyword : keywords) {
                    if (lowerCaseMessage.contains(keyword)) {
                        return true;
                    }
                }
            }
            current = current.getCause();
        }
        return false;
    }
}
