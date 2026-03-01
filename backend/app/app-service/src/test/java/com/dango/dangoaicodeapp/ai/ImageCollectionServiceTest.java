package com.dango.dangoaicodeapp.ai;

import com.dango.dangoaicodeapp.DangoAiCodeAppApplication;
import com.dango.dangoaicodeapp.domain.codegen.port.ImageCollectionPort;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author dango
 * @description
 * @date
 */
@SpringBootTest(classes = DangoAiCodeAppApplication.class,
        properties = {
                "dubbo.consumer.check=false",           // 禁用启动时检查
                "dubbo.registry.check=false",           // 禁用注册中心检查
                "dubbo.reference.check=false"           // 禁用引用检查
        })
@Disabled("依赖外部 AI 与工具服务，默认构建跳过")
class ImageCollectionServiceTest {

    @Resource
    private ImageCollectionPort imageCollectionService;

    @Test
    void testTechWebsiteImageCollection() {
        String result = imageCollectionService.collectImages("创建一个技术博客网站，需要展示编程教程和系统架构");
        Assertions.assertNotNull(result);
        System.out.println("技术网站收集到的图片: " + result);
    }

    @Test
    void testEcommerceWebsiteImageCollection() {
        String result = imageCollectionService.collectImages("创建一个电商购物网站，需要展示商品和品牌形象");
        Assertions.assertNotNull(result);
        System.out.println("电商网站收集到的图片: " + result);
    }
}
