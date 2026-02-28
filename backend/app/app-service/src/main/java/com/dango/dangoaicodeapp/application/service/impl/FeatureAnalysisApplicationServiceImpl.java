package com.dango.dangoaicodeapp.application.service.impl;

import cn.hutool.core.util.StrUtil;
import com.dango.dangoaicodeapp.application.assembler.FeatureAnalysisAssembler;
import com.dango.dangoaicodeapp.application.service.FeatureAnalysisApplicationService;
import com.dango.dangoaicodeapp.domain.codegen.model.FeatureAnalysis;
import com.dango.dangoaicodeapp.domain.codegen.model.FeatureAnalysisConstraints;
import com.dango.dangoaicodeapp.domain.codegen.service.FeatureAnalysisDomainService;
import com.dango.dangoaicodeapp.model.vo.FeatureAnalysisVO;
import com.dango.dangoaicodecommon.exception.ErrorCode;
import com.dango.dangoaicodecommon.exception.ThrowUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FeatureAnalysisApplicationServiceImpl implements FeatureAnalysisApplicationService {

    @Resource
    private FeatureAnalysisDomainService featureAnalysisDomainService;
    @Resource
    private FeatureAnalysisAssembler featureAnalysisAssembler;

    @Override
    public FeatureAnalysisVO analyzeFeatures(String prompt, String supplement) {
        ThrowUtils.throwIf(StrUtil.isBlank(prompt), ErrorCode.PARAMS_ERROR, "应用描述不能为空");
        ThrowUtils.throwIf(prompt.length() > FeatureAnalysisConstraints.MAX_PROMPT_LENGTH,
                ErrorCode.PARAMS_ERROR, "应用描述不能超过2000个字符");
        ThrowUtils.throwIf(StrUtil.isNotBlank(supplement)
                        && supplement.length() > FeatureAnalysisConstraints.MAX_SUPPLEMENT_LENGTH,
                ErrorCode.PARAMS_ERROR, "补充说明不能超过2000个字符");

        FeatureAnalysis featureAnalysis = featureAnalysisDomainService.analyzeFeatures(prompt, supplement);
        if (featureAnalysis == null) {
            featureAnalysis = FeatureAnalysis.of(List.of());
        }
        return featureAnalysisAssembler.toVO(featureAnalysis);
    }
}
