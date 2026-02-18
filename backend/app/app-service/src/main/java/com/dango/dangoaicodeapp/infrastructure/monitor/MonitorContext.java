package com.dango.dangoaicodeapp.infrastructure.monitor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 监控上下文
 * 用于在 ThreadLocal 中传递业务维度信息（如 appId、userId）
 *
 * @author dango
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonitorContext implements Serializable {

    private String userId;

    private String appId;

    @Serial
    private static final long serialVersionUID = 1L;
}
