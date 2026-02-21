package com.dango.dangoaicodecommon.log;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * Dubbo RPC IO 日志过滤器
 * 记录微服务之间调用的方法名、参数和耗时
 */
@Activate(group = {CommonConstants.PROVIDER, CommonConstants.CONSUMER}, order = -1000)
public class DubboIoLoggingFilter implements Filter {

    private static final Logger IO_LOG = LoggerFactory.getLogger("IO_LOG");
    private static final int MAX_PARAM_LENGTH = 1024;

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        String side = RpcContext.getServiceContext().isProviderSide() ? "PROVIDER" : "CONSUMER";
        String serviceName = invoker.getInterface().getSimpleName();
        String methodName = invocation.getMethodName();
        String params = truncate(Arrays.toString(invocation.getArguments()));

        long startTime = System.currentTimeMillis();
        Throwable error = null;
        try {
            Result result = invoker.invoke(invocation);
            if (result.hasException()) {
                error = result.getException();
            }
            return result;
        } catch (RpcException e) {
            error = e;
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;

            StringBuilder sb = new StringBuilder();
            sb.append("[DUBBO-").append(side).append("] ");
            sb.append(serviceName).append(".").append(methodName);
            sb.append(" | ").append(duration).append("ms");
            sb.append(" | params=").append(params);

            if (error != null) {
                sb.append(" | error=").append(error.getMessage());
                IO_LOG.warn(sb.toString());
            } else {
                IO_LOG.info(sb.toString());
            }
        }
    }

    private String truncate(String text) {
        if (text == null) {
            return "null";
        }
        return text.length() > MAX_PARAM_LENGTH
                ? text.substring(0, MAX_PARAM_LENGTH) + "...(truncated)"
                : text;
    }
}
