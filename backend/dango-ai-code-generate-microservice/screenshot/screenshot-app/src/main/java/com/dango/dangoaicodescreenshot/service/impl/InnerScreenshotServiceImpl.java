package com.dango.dangoaicodescreenshot.service.impl;

import com.dango.dangoaicodescreenshot.InnerScreenshotService;
import com.dango.dangoaicodescreenshot.service.ScreenshotService;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;

/**
 * 截图内部服务实现（对其他微服务提供）
 * 通过 Dubbo 暴露给其他微服务调用
 *
 * @author dango
 */
@Service
@DubboService
public class InnerScreenshotServiceImpl implements InnerScreenshotService {

    @Resource
    private ScreenshotService screenshotService;

    @Override
    public String generateAndUploadScreenshot(String webUrl) {
        return screenshotService.generateAndUploadScreenshot(webUrl);
    }
}
