package com.dango.supabase;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Supabase 服务启动类
 *
 * @author dango
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@ComponentScan(basePackages = {"com.dango.dangoaicodecommon", "com.dango.supabase"})
@EnableDubbo
public class SupabaseServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SupabaseServiceApplication.class, args);
    }
}
