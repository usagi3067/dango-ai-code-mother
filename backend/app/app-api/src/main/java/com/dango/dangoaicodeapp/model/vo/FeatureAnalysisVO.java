package com.dango.dangoaicodeapp.model.vo;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class FeatureAnalysisVO implements Serializable {
    private List<FeatureItemVO> features;
}
