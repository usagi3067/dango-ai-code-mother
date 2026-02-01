package com.dango.dangoaicodeapp.tools;

import com.dango.aicodegenerate.model.ImageCategoryEnum;
import com.dango.aicodegenerate.model.ImageResource;
import com.dango.aicodegenerate.tools.MermaidDiagramTool;
import com.dango.dangoaicodeapp.DangoAiCodeAppApplication;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = DangoAiCodeAppApplication.class,
        properties = {
                "dubbo.consumer.check=false",
                "dubbo.registry.check=false",
                "dubbo.reference.check=false"
        })
class MermaidDiagramToolTest {

    @Resource
    private MermaidDiagramTool mermaidDiagramTool;

    @Test
    void testGenerateMermaidDiagram() {
        String mermaidCode = """
                flowchart LR
                    Start([开始]) --> Input[输入数据]
                    Input --> Process[处理数据]
                    Process --> Decision{是否有效?}
                    Decision -->|是| Output[输出结果]
                    Decision -->|否| Error[错误处理]
                    Output --> End([结束])
                    Error --> End
                """;
        String description = "简单系统架构图";
        List<ImageResource> diagrams = mermaidDiagramTool.generateMermaidDiagram(mermaidCode, description);
        assertNotNull(diagrams);
        
        ImageResource firstDiagram = diagrams.get(0);
        assertEquals(ImageCategoryEnum.ARCHITECTURE, firstDiagram.getCategory());
        assertEquals(description, firstDiagram.getDescription());
        assertNotNull(firstDiagram.getUrl());
        assertTrue(firstDiagram.getUrl().startsWith("http"));
        System.out.println("生成了架构图: " + firstDiagram.getUrl());
    }
}
