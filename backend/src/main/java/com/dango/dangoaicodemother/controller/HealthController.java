package com.dango.dangoaicodemother.controller;

import com.dango.dangoaicodemother.common.BaseResponse;
import com.dango.dangoaicodemother.common.ResultUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author dango
 * @description 健康检查
 */
@RestController
@RequestMapping("/health")
public class HealthController {

    @GetMapping("/")
    public BaseResponse<String> healthCheck() {
        return ResultUtils.success("ok");
    }
}
