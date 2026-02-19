package com.dango.dangoaicodeapp.model.dto.app;

import lombok.Data;
import java.io.Serializable;

@Data
public class AnalyzeFeaturesRequest implements Serializable {
    private String prompt;
    private String supplement;
}
