package com.dango.dangoaicodeapp.tools;

import com.dango.aicodegenerate.model.ImageCategoryEnum;
import com.dango.aicodegenerate.model.ImageResource;
import com.dango.dangoaicodeapp.domain.codegen.tools.UndrawIllustrationTool;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = UndrawIllustrationTool.class)
class UndrawIllustrationToolTest {

    @Resource
    private UndrawIllustrationTool undrawIllustrationTool;

    @Test
    void testSearchIllustrations() {
        List<ImageResource> illustrations = undrawIllustrationTool.searchIllustrations("happy");
        assertNotNull(illustrations);
        
        System.out.println("搜索到 " + illustrations.size() + " 张插画");
        
        if (!illustrations.isEmpty()) {
            ImageResource firstIllustration = illustrations.get(0);
            assertEquals(ImageCategoryEnum.ILLUSTRATION, firstIllustration.getCategory());
            assertNotNull(firstIllustration.getDescription());
            assertNotNull(firstIllustration.getUrl());
            assertTrue(firstIllustration.getUrl().startsWith("http"));
            
            illustrations.forEach(illustration -> 
                System.out.println("插画: " + illustration.getDescription() + " - " + illustration.getUrl())
            );
        } else {
            System.out.println("警告: API 未返回任何插画结果");
        }
    }
}
