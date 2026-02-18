package com.dango.dangoaicodecommon.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> {
            SaRouter.match("/**").check(r -> StpUtil.checkLogin());
        })).addPathPatterns("/**")
           .excludePathPatterns(
               "/user/login",
               "/user/register",
               "/user/get/login",
               "/app/good/list/page/vo",
               "/app/list/cursor/vo",
               "/app/get/vo",
               "/doc.html", "/webjars/**", "/v3/api-docs/**",
               "/actuator/**"
           );
    }
}
