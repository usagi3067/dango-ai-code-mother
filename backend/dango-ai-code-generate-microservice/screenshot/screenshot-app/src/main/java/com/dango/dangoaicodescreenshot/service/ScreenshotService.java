package com.dango.dangoaicodescreenshot.service;

import com.dango.dangoaicodescreenshot.InnerScreenshotService;

/**
 * 截图服务层（本服务内部使用）
 * 对其他微服务提供的接口请使用 {@link InnerScreenshotService}
 *
 * @author dango
 */
public interface ScreenshotService {

    /**
     * 生成网页截图并上传到对象存储
     *
     * @param webUrl 网页 URL
     * @return 截图图片的 URL
     */
    String generateAndUploadScreenshot(String webUrl);
}
