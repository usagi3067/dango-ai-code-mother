package com.dango.dangoaicodescreenshot;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * @author dango
 * @description
 * @date
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.dango.dangoaicodecommon", "com.dango.dangoaicodescreenshot"})
@EnableDubbo
public class DangoAiCodeScreenshotApplication {
    public static void main(String[] args) {
        SpringApplication.run(DangoAiCodeScreenshotApplication.class, args);
    }
}
