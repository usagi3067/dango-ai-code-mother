package com.dango.dangoaicodeapp.model.vo;

import lombok.Data;
import java.io.Serializable;

@Data
public class FeatureItemVO implements Serializable {
    private String name;
    private String description;
    private boolean checked;
    private boolean recommended;
}
