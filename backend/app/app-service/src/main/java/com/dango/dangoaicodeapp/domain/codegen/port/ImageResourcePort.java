package com.dango.dangoaicodeapp.domain.codegen.port;

import com.dango.aicodegenerate.model.ImageResource;

import java.util.List;

/**
 * 图片资源端口。
 */
public interface ImageResourcePort {

    List<ImageResource> searchContentImages(String query);

    List<ImageResource> searchIllustrations(String query);

    List<ImageResource> generateMermaidDiagram(String mermaidCode, String description);

    List<ImageResource> generateLogos(String description);
}
