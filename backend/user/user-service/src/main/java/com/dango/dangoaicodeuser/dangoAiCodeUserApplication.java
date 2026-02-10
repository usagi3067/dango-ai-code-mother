package com.dango.dangoaicodeuser;

import com.mybatisflex.spring.boot.MyBatisFlexCustomizer;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * @author dango
 * @description
 * @date
 */
@SpringBootApplication
@EnableAspectJAutoProxy(exposeProxy = true)
@MapperScan("com.dango.dangoaicodeuser.mapper")
@ComponentScan(basePackages = {"com.dango.dangoaicodecommon", "com.dango.dangoaicodeuser"})
@EnableDubbo
public class dangoAiCodeUserApplication {

    public static void main(String[] args) {
        SpringApplication.run(dangoAiCodeUserApplication.class, args);
    }
}
