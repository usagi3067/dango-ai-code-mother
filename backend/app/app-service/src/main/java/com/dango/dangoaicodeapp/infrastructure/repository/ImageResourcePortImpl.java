package com.dango.dangoaicodeapp.infrastructure.repository;

import com.dango.aicodegenerate.model.ImageResource;
import com.dango.dangoaicodeapp.domain.codegen.port.ImageResourcePort;
import com.dango.dangoaicodeapp.domain.codegen.tools.ImageSearchTool;
import com.dango.dangoaicodeapp.domain.codegen.tools.LogoGeneratorTool;
import com.dango.dangoaicodeapp.domain.codegen.tools.MermaidDiagramTool;
import com.dango.dangoaicodeapp.domain.codegen.tools.UndrawIllustrationTool;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 图片资源端口适配器。
 */
@Component
public class ImageResourcePortImpl implements ImageResourcePort {

    @Resource
    private ImageSearchTool imageSearchTool;

    @Resource
    private UndrawIllustrationTool undrawIllustrationTool;

    @Resource
    private MermaidDiagramTool mermaidDiagramTool;

    @Resource
    private LogoGeneratorTool logoGeneratorTool;

    @Override
    public List<ImageResource> searchContentImages(String query) {
        return imageSearchTool.searchContentImages(query);
    }

    @Override
    public List<ImageResource> searchIllustrations(String query) {
        return undrawIllustrationTool.searchIllustrations(query);
    }

    @Override
    public List<ImageResource> generateMermaidDiagram(String mermaidCode, String description) {
        return mermaidDiagramTool.generateMermaidDiagram(mermaidCode, description);
    }

    @Override
    public List<ImageResource> generateLogos(String description) {
        return logoGeneratorTool.generateLogos(description);
    }
}
