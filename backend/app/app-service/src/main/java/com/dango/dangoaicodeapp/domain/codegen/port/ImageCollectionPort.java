package com.dango.dangoaicodeapp.domain.codegen.port;

import com.dango.aicodegenerate.model.ImageCollectionPlan;

/**
 * 图片规划与收集能力领域端口。
 */
public interface ImageCollectionPort {

    ImageCollectionPlan planImageCollection(String userPrompt);

    String collectImages(String userPrompt);
}
