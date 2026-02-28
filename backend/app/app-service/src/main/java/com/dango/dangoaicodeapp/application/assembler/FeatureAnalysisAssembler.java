package com.dango.dangoaicodeapp.application.assembler;

import com.dango.dangoaicodeapp.domain.codegen.model.AnalyzedFeature;
import com.dango.dangoaicodeapp.domain.codegen.model.FeatureAnalysis;
import com.dango.dangoaicodeapp.model.vo.FeatureAnalysisVO;
import com.dango.dangoaicodeapp.model.vo.FeatureItemVO;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 功能分析对象转换器。
 */
@Component
public class FeatureAnalysisAssembler {

    public FeatureAnalysisVO toVO(FeatureAnalysis featureAnalysis) {
        FeatureAnalysisVO vo = new FeatureAnalysisVO();
        List<FeatureItemVO> features = featureAnalysis.features().stream()
                .map(this::toFeatureItemVO)
                .toList();
        vo.setFeatures(features);
        return vo;
    }

    private FeatureItemVO toFeatureItemVO(AnalyzedFeature item) {
        FeatureItemVO itemVO = new FeatureItemVO();
        itemVO.setName(item.name());
        itemVO.setDescription(item.description());
        itemVO.setChecked(item.checked());
        itemVO.setRecommended(item.recommended());
        return itemVO;
    }
}
