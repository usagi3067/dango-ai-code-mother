package com.dango.dangoaicodemother;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy(exposeProxy = true)
public class DangoAiCodeMotherApplication {

    public static void main(String[] args) {
        SpringApplication.run(DangoAiCodeMotherApplication.class, args);
    }

}
