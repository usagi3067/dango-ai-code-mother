package com.dango.dangoaicodeuser;

import com.mybatisflex.spring.boot.MyBatisFlexCustomizer;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * @author dango
 * @description
 * @date
 */
@SpringBootApplication
@EnableAspectJAutoProxy(exposeProxy = true)
@MapperScan("com.dango.dangoaicodeuser.mapper")
public class dangoAiCodeUserApplication {

    public static void main(String[] args) {
        SpringApplication.run(dangoAiCodeUserApplication.class, args);
    }
}
