package com.dango.dangoaicodecommon.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import com.dango.dangoaicodecommon.log.IoLoggingInterceptor;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {

    private final IoLoggingInterceptor ioLoggingInterceptor;

    public SaTokenConfigure(IoLoggingInterceptor ioLoggingInterceptor) {
        this.ioLoggingInterceptor = ioLoggingInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // IO 日志拦截器
        registry.addInterceptor(ioLoggingInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/actuator/**");
        registry.addInterceptor(new SaInterceptor(handle -> {
            SaRouter.match("/**").check(r -> StpUtil.checkLogin());
        }) {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
                // SSE 等异步请求完成后 Tomcat 会做 async dispatch，此时 SaToken ThreadLocal 上下文已丢失，跳过鉴权
                if (request.getDispatcherType() == DispatcherType.ASYNC) {
                    return true;
                }
                return super.preHandle(request, response, handler);
            }
        }).addPathPatterns("/**")
           .excludePathPatterns(
               "/user/login",
               "/user/register",
               "/user/get/login",
               "/app/good/list/page/vo",
               "/app/list/cursor/vo",
               "/app/get/vo",
               "/doc.html", "/webjars/**", "/v3/api-docs/**", "/favicon.ico",
               "/actuator/**"
           );
    }
}
