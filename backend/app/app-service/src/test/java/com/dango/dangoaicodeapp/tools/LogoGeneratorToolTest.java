package com.dango.dangoaicodeapp.tools;

import com.dango.aicodegenerate.model.ImageCategoryEnum;
import com.dango.aicodegenerate.model.ImageResource;
import com.dango.dangoaicodeapp.domain.codegen.tools.LogoGeneratorTool;
import com.dango.dangoaicodeapp.DangoAiCodeAppApplication;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = DangoAiCodeAppApplication.class,
        properties = {
                "dubbo.consumer.check=false",
                "dubbo.registry.check=false",
                "dubbo.reference.check=false"
        })
class LogoGeneratorToolTest {

    @Resource
    private LogoGeneratorTool logoGeneratorTool;

    @Test
    void testGenerateLogos() {
        List<ImageResource> logos = logoGeneratorTool.generateLogos("技术公司现代简约风格Logo");
        assertNotNull(logos);
        ImageResource firstLogo = logos.getFirst();
        assertEquals(ImageCategoryEnum.LOGO, firstLogo.getCategory());
        assertNotNull(firstLogo.getDescription());
        assertNotNull(firstLogo.getUrl());
        logos.forEach(logo ->
                System.out.println("Logo: " + logo.getDescription() + " - " + logo.getUrl())
        );
    }
}
